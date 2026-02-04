package com.cs544.aichat.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Sorts;

@Service
public class SystemRagContextService {
    private final MongoClient mongoClient;
    private final String releasesDb;
    private final String discussionsDb;
    private final String notificationsDb;
    private final int releaseLimit;
    private final int discussionLimit;
    private final int notificationLimit;

    public SystemRagContextService(
            MongoClient mongoClient,
            @Value("${rag.mongo.releases-db:releases}") String releasesDb,
            @Value("${rag.mongo.discussions-db:discussions}") String discussionsDb,
            @Value("${rag.mongo.notifications-db:notifications}") String notificationsDb,
            @Value("${rag.limit.releases:4}") int releaseLimit,
            @Value("${rag.limit.discussions:6}") int discussionLimit,
            @Value("${rag.limit.notifications:6}") int notificationLimit
    ) {
        this.mongoClient = mongoClient;
        this.releasesDb = releasesDb;
        this.discussionsDb = discussionsDb;
        this.notificationsDb = notificationsDb;
        this.releaseLimit = releaseLimit;
        this.discussionLimit = discussionLimit;
        this.notificationLimit = notificationLimit;
    }

    public String buildSystemContext(String prompt, String userId) {
        List<String> terms = extractTerms(prompt);
        String releases = buildReleaseContext(terms, userId);
        String discussions = buildDiscussionContext(terms, userId);
        String notifications = buildNotificationContext(terms, userId);
        return String.join("\n\n",
                "System RAG context",
                releases,
                discussions,
                notifications);
    }

    public String summarizeUserTasks(String userId) {
        var collection = mongoClient.getDatabase(releasesDb).getCollection("releases");
        List<Document> releases = collection.find()
                .sort(Sorts.descending("createdAt"))
                .limit(60)
                .into(new ArrayList<>());
        if (releases.isEmpty()) {
            return "No release records found.";
        }

        List<String> taskLines = new ArrayList<>();
        Set<String> assignees = new HashSet<>();
        int totalTasks = 0;
        int inProcess = 0;
        int todo = 0;
        int done = 0;

        for (Document release : releases) {
            String releaseId = stringValue(release.get("_id"));
            String releaseName = stringValue(release.get("name"));
            String releaseVersion = stringValue(release.get("version"));
            List<Map<String, Object>> tasks = listOfMap(release.get("tasks"));

            for (Map<String, Object> task : tasks) {
                String assignee = stringValue(task.get("assigneeId"));
                if (!assignee.isBlank()) {
                    assignees.add(assignee);
                }
                if (!matchesUser(assignee, userId)) {
                    continue;
                }
                totalTasks++;
                String status = stringValue(task.get("status"));
                if ("IN_PROCESS".equals(status)) {
                    inProcess++;
                } else if ("TODO".equals(status)) {
                    todo++;
                } else if ("DONE".equals(status)) {
                    done++;
                }
                if (taskLines.size() < 8) {
                    taskLines.add(String.format(
                            "%d. %s\n   id: %s\n   status: %s\n   release: %s v%s",
                            taskLines.size() + 1,
                            displayTitle(task),
                            firstNonBlank(stringValue(task.get("id")), stringValue(task.get("_id"))),
                            status,
                            releaseName,
                            releaseVersion
                    ));
                }
            }
        }

        if (totalTasks == 0) {
            String assigneeSample = assignees.isEmpty() ? "none" : String.join(", ", assignees.stream().limit(6).toList());
            return String.join("\n",
                    "Task Summary",
                    "User: " + userId,
                    "Assigned: 0",
                    "",
                    "No tasks are currently assigned to you.",
                    "Known assignees in system: " + assigneeSample
            );
        }

        List<String> lines = new ArrayList<>();
        lines.add("Task Summary");
        lines.add("User: " + userId);
        lines.add(String.format(
                "Assigned: %d | In Progress: %d | TODO: %d | Done: %d",
                totalTasks, inProcess, todo, done
        ));
        lines.add("");
        lines.add("Tasks:");
        lines.addAll(taskLines);
        return String.join("\n", lines);
    }

    public String summarizeUserTasksHtml(String userId) {
        var collection = mongoClient.getDatabase(releasesDb).getCollection("releases");
        List<Document> releases = collection.find()
                .sort(Sorts.descending("createdAt"))
                .limit(60)
                .into(new ArrayList<>());

        List<TaskSummaryItem> items = new ArrayList<>();
        int inProcess = 0;
        int todo = 0;
        int done = 0;
        for (Document release : releases) {
            String releaseId = stringValue(release.get("_id"));
            String releaseName = stringValue(release.get("name"));
            String releaseVersion = stringValue(release.get("version"));
            List<Map<String, Object>> tasks = listOfMap(release.get("tasks"));
            for (Map<String, Object> task : tasks) {
                if (!matchesUser(stringValue(task.get("assigneeId")), userId)) {
                    continue;
                }
                String status = stringValue(task.get("status"));
                if ("IN_PROCESS".equals(status)) {
                    inProcess++;
                } else if ("TODO".equals(status)) {
                    todo++;
                } else if ("DONE".equals(status)) {
                    done++;
                }
                items.add(new TaskSummaryItem(
                        firstNonBlank(stringValue(task.get("id")), stringValue(task.get("_id"))),
                        releaseId,
                        displayTitle(task),
                        status,
                        releaseName,
                        releaseVersion
                ));
            }
        }

        StringBuilder html = new StringBuilder();
        html.append("<section class=\"chat-task-summary\">");
        html.append("<h3>Task Summary</h3>");
        html.append("<div class=\"chat-task-meta\">");
        html.append("<span>User: ").append(escapeHtml(userId)).append("</span>");
        html.append("<span>Assigned: ").append(items.size()).append("</span>");
        html.append("<span>In Progress: ").append(inProcess).append("</span>");
        html.append("<span>TODO: ").append(todo).append("</span>");
        html.append("<span>Done: ").append(done).append("</span>");
        html.append("</div>");

        if (items.isEmpty()) {
            html.append("<p class=\"chat-task-empty\">No tasks are currently assigned to you.</p>");
            html.append("</section>");
            return html.toString();
        }

        html.append("<div class=\"chat-task-grid\">");
        for (TaskSummaryItem item : items.stream().limit(12).toList()) {
            html.append("<button type=\"button\" class=\"chat-task-card\" ")
                    .append("data-chat-task-id=\"").append(escapeHtml(item.taskId())).append("\" ")
                    .append("data-chat-release-id=\"").append(escapeHtml(item.releaseId())).append("\">")
                    .append("<strong>").append(escapeHtml(item.title())).append("</strong>")
                    .append("<span>Status: ").append(escapeHtml(item.status())).append("</span>")
                    .append("<span>Release: ")
                    .append(escapeHtml(item.releaseName()))
                    .append(" v")
                    .append(escapeHtml(item.releaseVersion()))
                    .append("</span>")
                    .append("</button>");
        }
        html.append("</div>");
        html.append("</section>");
        return html.toString();
    }

    private String buildReleaseContext(List<String> terms, String userId) {
        var collection = mongoClient.getDatabase(releasesDb).getCollection("releases");
        List<Document> candidates = collection.find()
                .sort(Sorts.descending("createdAt"))
                .limit(40)
                .into(new ArrayList<>());
        List<Document> docs = pickTopRelevant(candidates, terms, userId, releaseLimit, "tasks.assigneeId");
        if (docs.isEmpty()) {
            return "Releases: no matching release/task records.";
        }

        List<String> lines = new ArrayList<>();
        List<Map<String, Object>> userTasks = new ArrayList<>();
        for (Document doc : docs) {
            String releaseId = stringValue(doc.get("_id"));
            String name = stringValue(doc.get("name"));
            String version = stringValue(doc.get("version"));
            boolean completed = Boolean.TRUE.equals(doc.get("completed"));
            List<Map<String, Object>> tasks = listOfMap(doc.get("tasks"));
            userTasks.addAll(tasks.stream()
                    .filter(task -> matchesUser(stringValue(task.get("assigneeId")), userId))
                    .collect(Collectors.toList()));
            long inProcess = tasks.stream()
                    .filter(task -> "IN_PROCESS".equals(stringValue(task.get("status"))))
                    .count();
            lines.add(String.format(
                    "Release %s (%s v%s) completed=%s tasks=%d in_process=%d",
                    releaseId, name, version, completed, tasks.size(), inProcess
            ));

            tasks.stream()
                    .filter(task -> userId.equals(stringValue(task.get("assigneeId"))) || matchesAny(task, terms))
                    .limit(2)
                    .forEach(task -> lines.add(String.format(
                            "  Task %s title=%s assignee=%s status=%s",
                            firstNonBlank(stringValue(task.get("id")), stringValue(task.get("_id"))),
                            summarize(stringValue(task.get("title")), 80),
                            stringValue(task.get("assigneeId")),
                            stringValue(task.get("status"))
                    )));
        }
        long userInProcess = userTasks.stream()
                .filter(task -> "IN_PROCESS".equals(stringValue(task.get("status"))))
                .count();
        long userTodo = userTasks.stream()
                .filter(task -> "TODO".equals(stringValue(task.get("status"))))
                .count();
        long userDone = userTasks.stream()
                .filter(task -> "DONE".equals(stringValue(task.get("status"))))
                .count();
        lines.add(0, userTasks.isEmpty()
                ? "User task summary: no tasks assigned to this user in release records."
                : String.format(
                        "User task summary: assigned=%d in_process=%d todo=%d done=%d",
                        userTasks.size(), userInProcess, userTodo, userDone
                ));
        return "Releases:\n" + String.join("\n", lines);
    }

    private String buildDiscussionContext(List<String> terms, String userId) {
        var collection = mongoClient.getDatabase(discussionsDb).getCollection("discussion_messages");
        List<Document> candidates = collection.find()
                .sort(Sorts.descending("createdAt"))
                .limit(50)
                .into(new ArrayList<>());
        List<Document> docs = pickTopRelevant(candidates, terms, userId, discussionLimit, "author");
        if (docs.isEmpty()) {
            return "Discussions: no matching messages.";
        }

        List<String> lines = docs.stream()
                .map(doc -> String.format(
                        "[%s] author=%s task=%s release=%s msg=%s",
                        stringValue(doc.get("createdAt")),
                        stringValue(doc.get("author")),
                        stringValue(doc.get("taskId")),
                        stringValue(doc.get("releaseId")),
                        summarize(stringValue(doc.get("message")), 140)
                ))
                .collect(Collectors.toList());
        return "Discussions:\n" + String.join("\n", lines);
    }

    private String buildNotificationContext(List<String> terms, String userId) {
        var collection = mongoClient.getDatabase(notificationsDb).getCollection("NotificationLog");
        List<Document> candidates = collection.find()
                .sort(Sorts.descending("createdAt"))
                .limit(60)
                .into(new ArrayList<>());
        List<Document> docs = pickTopRelevant(candidates, terms, userId, notificationLimit, "recipient");
        if (docs.isEmpty()) {
            return "Notifications: no matching logs.";
        }

        List<String> lines = docs.stream()
                .filter(doc ->
                        userId.equals(stringValue(doc.get("recipient")))
                                || stringValue(doc.get("recipient")).toLowerCase(Locale.ROOT).contains(userId.toLowerCase(Locale.ROOT))
                                || matchesAny(doc, terms))
                .limit(notificationLimit)
                .map(doc -> String.format(
                        "[%s] type=%s recipient=%s status=%s subject=%s",
                        stringValue(doc.get("createdAt")),
                        stringValue(doc.get("eventType")),
                        stringValue(doc.get("recipient")),
                        stringValue(doc.get("status")),
                        summarize(stringValue(doc.get("subject")), 90)
                ))
                .collect(Collectors.toList());
        if (lines.isEmpty()) {
            return "Notifications: logs exist, but no user/term-relevant entries.";
        }
        return "Notifications:\n" + String.join("\n", lines);
    }

    private List<String> extractTerms(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String raw : prompt.toLowerCase(Locale.ROOT).split("[^a-z0-9-]+")) {
            if (raw.length() >= 3) {
                terms.add(raw);
            }
            if (terms.size() >= 6) {
                break;
            }
        }
        return new ArrayList<>(terms);
    }

    private List<Document> pickTopRelevant(
            List<Document> candidates,
            List<String> terms,
            String userId,
            int limit,
            String primaryUserField
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<ScoredDocument> scored = candidates.stream()
                .map(doc -> new ScoredDocument(doc, score(doc, terms, userId, primaryUserField)))
                .sorted(Comparator.comparingInt(ScoredDocument::score).reversed())
                .collect(Collectors.toList());

        List<Document> matched = scored.stream()
                .filter(item -> item.score() > 0)
                .map(ScoredDocument::document)
                .limit(limit)
                .collect(Collectors.toList());
        if (!matched.isEmpty()) {
            return matched;
        }
        return candidates.stream().limit(Math.min(3, limit)).collect(Collectors.toList());
    }

    private int score(Document doc, List<String> terms, String userId, String primaryUserField) {
        int score = 0;
        String flattened = flatten(doc).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            String needle = term.toLowerCase(Locale.ROOT);
            if (flattened.contains(needle)) {
                score += 3;
            }
        }
        String userNeedle = userId == null ? "" : userId.toLowerCase(Locale.ROOT);
        List<String> primaryUserValues = resolvePathValues(doc, primaryUserField).stream()
                .map(value -> stringValue(value).toLowerCase(Locale.ROOT))
                .toList();
        boolean userMatch = !userNeedle.isBlank()
                && primaryUserValues.stream().anyMatch(value -> matchesUser(value, userNeedle));
        if (userMatch) {
            score += 8;
        }
        if (terms.isEmpty()) {
            score += 1;
        }
        return score;
    }

    private List<Object> resolvePathValues(Map<String, Object> map, String dotPath) {
        if (map == null || dotPath == null || dotPath.isBlank()) {
            return List.of();
        }
        List<Object> results = new ArrayList<>();
        resolvePathValuesRecursive(map, dotPath.split("\\."), 0, results);
        return results;
    }

    private void resolvePathValuesRecursive(Object current, String[] parts, int index, List<Object> results) {
        if (current == null) {
            return;
        }
        if (index >= parts.length) {
            results.add(current);
            return;
        }
        if (current instanceof Map<?, ?> currentMap) {
            resolvePathValuesRecursive(currentMap.get(parts[index]), parts, index + 1, results);
            return;
        }
        if (current instanceof List<?> list) {
            for (Object item : list) {
                resolvePathValuesRecursive(item, parts, index, results);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String flatten(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().map(this::flatten).collect(Collectors.joining(" "));
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::flatten).collect(Collectors.joining(" "));
        }
        return String.valueOf(value);
    }

    private String summarize(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMap(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private boolean matchesAny(Map<String, Object> record, List<String> terms) {
        if (terms.isEmpty()) {
            return true;
        }
        String flattened = record.values().stream().map(this::stringValue).collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT);
        return terms.stream().anyMatch(flattened::contains);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private boolean matchesUser(String candidate, String userId) {
        if (candidate == null || userId == null || userId.isBlank()) {
            return false;
        }
        String candidateValue = candidate.toLowerCase(Locale.ROOT);
        String userNeedle = userId.toLowerCase(Locale.ROOT);
        return candidateValue.equals(userNeedle);
    }

    private String displayTitle(Map<String, Object> task) {
        String title = summarize(stringValue(task.get("title")), 80);
        return title.isBlank() ? "(Untitled task)" : title;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record ScoredDocument(Document document, int score) {
    }

    private record TaskSummaryItem(
            String taskId,
            String releaseId,
            String title,
            String status,
            String releaseName,
            String releaseVersion
    ) {
    }
}
