const { useEffect, useState } = React;

const baseUrls = {
  releases: "http://localhost:8081",
  discussions: "http://localhost:8082",
  chat: "http://localhost:8083",
};

const SectionCard = ({ title, tone, children }) => (
  <div className={`card ${tone || ""}`.trim()}>
    <h2>{title}</h2>
    <div className="stack">{children}</div>
  </div>
);

const App = () => {
  const [loginForm, setLoginForm] = useState({
    role: "release-manager",
    remember: true,
  });
  const [role, setRole] = useState("release-manager");
  const [loggedIn, setLoggedIn] = useState(false);
  const [tokens, setTokens] = useState({});
  const [releaseForm, setReleaseForm] = useState({ name: "Album", version: "1.0" });
  const [releases, setReleases] = useState([]);
  const [selectedReleaseId, setSelectedReleaseId] = useState("");
  const [expandedReleaseId, setExpandedReleaseId] = useState("");
  const [selectedTaskReleaseId, setSelectedTaskReleaseId] = useState("");
  const [selectedTaskId, setSelectedTaskId] = useState("");
  const [taskForm, setTaskForm] = useState({
    title: "",
    description: "",
    assigneeId: "release-manager",
    orderIndex: 1,
  });
  const [discussionForm, setDiscussionForm] = useState({
    releaseId: "",
    author: "release-manager",
    message: "",
  });
  const [discussionTaskId, setDiscussionTaskId] = useState("");
  const [discussionMessages, setDiscussionMessages] = useState([]);
  const [replyParentId, setReplyParentId] = useState("");
  const [chatPrompt, setChatPrompt] = useState("Summarize");
  const [status, setStatus] = useState("Choose a role to start your local session.");
  const [activeView, setActiveView] = useState("menu");
  const [isReleaseModalOpen, setIsReleaseModalOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [isErrorOpen, setIsErrorOpen] = useState(false);

  const updateStatus = (message) => setStatus(message);
  const showError = (message) => {
    setErrorMessage(message);
    setIsErrorOpen(true);
    updateStatus(message);
  };

  const roleLabel = {
    "release-manager": "Release manager",
    "dev-1": "Developer 1",
    "dev-2": "Developer 2",
  };

  const handleLogin = () => {
    const selectedRole = loginForm.role;
    if (!selectedRole) {
      updateStatus("Select a role to continue.");
      return;
    }
    setRole(selectedRole);
    setLoggedIn(true);
    setActiveView("menu");
    updateStatus(`Signed in locally as ${roleLabel[selectedRole] || selectedRole}.`);
  };

  const handleLogout = () => {
    setLoggedIn(false);
    setTokens({});
    setReleases([]);
    setSelectedReleaseId("");
    setExpandedReleaseId("");
    setSelectedTaskReleaseId("");
    setSelectedTaskId("");
    setActiveView("menu");
    updateStatus("You have been signed out of the local session.");
  };

  useEffect(() => {
    document.body.dataset.view = activeView;
    document.body.dataset.role = role;
  }, [activeView, role]);

  useEffect(() => {
    setTaskForm((prev) => ({ ...prev, assigneeId: role }));
    setDiscussionForm((prev) => ({ ...prev, author: role }));
  }, [role]);

  useEffect(() => {
    if (!selectedReleaseId) {
      return;
    }
    setDiscussionForm((prev) => ({ ...prev, releaseId: selectedReleaseId }));
    const release = releases.find((item) => item.id === selectedReleaseId);
    const nextIndex =
      release && release.tasks && release.tasks.length > 0
        ? Math.max(...release.tasks.map((task) => task.orderIndex)) + 1
        : 1;
    setTaskForm((prev) => ({ ...prev, orderIndex: nextIndex }));
  }, [selectedReleaseId, releases]);

  useEffect(() => {
    if (selectedReleaseId) {
      setExpandedReleaseId(selectedReleaseId);
    }
    setSelectedTaskReleaseId(selectedReleaseId);
    setSelectedTaskId("");
  }, [selectedReleaseId]);

  const requestToken = async (service) => {
    updateStatus(`Requesting ${service} token...`);
    try {
      const response = await fetch(`${baseUrls[service]}/api/${service}/token`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: role }),
      });
      if (!response.ok) {
        throw new Error(`${service} token request failed: ${response.status}`);
      }
      const data = await response.json();
      const token = data.token || data.accessToken || "";
      setTokens((prev) => ({ ...prev, [service]: token }));
      updateStatus(`${service} token ready.`);
      return token;
    } catch (error) {
      showError(error.message);
      throw error;
    }
  };

  const loadReleases = async (selectFirst = true) => {
    updateStatus("Loading releases...");
    try {
      const token = tokens.releases || (await requestToken("releases"));
      const response = await fetch(`${baseUrls.releases}/api/releases`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        throw new Error(`Release list failed: ${response.status}`);
      }
      const data = await response.json();
      setReleases(data);
      if (selectFirst) {
        setSelectedReleaseId(data[0]?.id || "");
      }
      updateStatus("Releases loaded.");
    } catch (error) {
      showError(error.message);
    }
  };

  const submitRelease = async () => {
    updateStatus("Creating release...");
    try {
      const token = tokens.releases || (await requestToken("releases"));
      const response = await fetch(`${baseUrls.releases}/api/releases`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(releaseForm),
      });
      if (!response.ok) {
        throw new Error(`Release create failed: ${response.status}`);
      }
      const created = await response.json();
      updateStatus("Release created successfully.");
      setReleaseForm({ name: "", version: "" });
      await loadReleases(false);
      setSelectedReleaseId(created.id);
      setIsReleaseModalOpen(false);
    } catch (error) {
      showError(error.message);
    }
  };

  const addTask = async (releaseIdOverride) => {
    const targetReleaseId = releaseIdOverride || selectedReleaseId;
    if (!targetReleaseId) {
      updateStatus("Select a release before adding tasks.");
      return;
    }
    updateStatus("Adding task...");
    try {
      const token = tokens.releases || (await requestToken("releases"));
      const payload = {
        title: taskForm.title,
        description: taskForm.description,
        assigneeId: taskForm.assigneeId,
        orderIndex: Number(taskForm.orderIndex),
      };
      const response = await fetch(`${baseUrls.releases}/api/releases/${targetReleaseId}/tasks`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        const errorPayload = await response.json().catch(() => ({}));
        throw new Error(errorPayload.message || `Task create failed: ${response.status}`);
      }
      updateStatus("Task added.");
      setTaskForm((prev) => ({
        ...prev,
        title: "",
        description: "",
        orderIndex: Number(prev.orderIndex) + 1,
      }));
      await loadReleases(false);
    } catch (error) {
      showError(error.message);
    }
  };

  const startTask = async (releaseId, taskId) => {
    updateStatus("Starting task...");
    try {
      const token = tokens.releases || (await requestToken("releases"));
      const response = await fetch(
        `${baseUrls.releases}/api/releases/${releaseId}/tasks/${taskId}/start`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ developerId: role }),
        }
      );
      if (!response.ok) {
        const errorPayload = await response.json().catch(() => ({}));
        throw new Error(errorPayload.message || `Start failed: ${response.status}`);
      }
      updateStatus("Task started.");
      await loadReleases(false);
    } catch (error) {
      showError(error.message);
    }
  };

  const completeTask = async (releaseId, taskId) => {
    updateStatus("Completing task...");
    try {
      const token = tokens.releases || (await requestToken("releases"));
      const response = await fetch(
        `${baseUrls.releases}/api/releases/${releaseId}/tasks/${taskId}/complete`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ developerId: role }),
        }
      );
      if (!response.ok) {
        const errorPayload = await response.json().catch(() => ({}));
        throw new Error(errorPayload.message || `Complete failed: ${response.status}`);
      }
      updateStatus("Task completed.");
      await loadReleases(false);
    } catch (error) {
      showError(error.message);
    }
  };

  const completeRelease = async (releaseIdOverride) => {
    const targetReleaseId = releaseIdOverride || selectedReleaseId;
    if (!targetReleaseId) {
      updateStatus("Select a release before completing.");
      return;
    }
    updateStatus("Completing release...");
    try {
      const token = tokens.releases || (await requestToken("releases"));
      const response = await fetch(`${baseUrls.releases}/api/releases/${targetReleaseId}/complete`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) {
        const errorPayload = await response.json().catch(() => ({}));
        throw new Error(errorPayload.message || `Release complete failed: ${response.status}`);
      }
      updateStatus("Release completed.");
      await loadReleases(false);
    } catch (error) {
      showError(error.message);
    }
  };

  const submitDiscussion = async () => {
    if (!discussionTaskId) {
      showError("Select a task to start the discussion.");
      return;
    }
    updateStatus("Posting discussion message...");
    try {
      const token = tokens.discussions || (await requestToken("discussions"));
      const response = await fetch(`${baseUrls.discussions}/api/discussions`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          releaseId: discussionForm.releaseId,
          taskId: discussionTaskId,
          parentId: replyParentId || null,
          author: discussionForm.author,
          message: discussionForm.message,
        }),
      });
      if (!response.ok) {
        throw new Error(`Discussion create failed: ${response.status}`);
      }
      updateStatus("Discussion message posted.");
      setDiscussionForm((prev) => ({ ...prev, message: "" }));
      setReplyParentId("");
      await loadDiscussionMessages(discussionTaskId);
    } catch (error) {
      showError(error.message);
    }
  };

  const loadDiscussionMessages = async (taskId) => {
    if (!taskId) {
      return;
    }
    updateStatus("Loading discussion thread...");
    try {
      const token = tokens.discussions || (await requestToken("discussions"));
      const response = await fetch(`${baseUrls.discussions}/api/discussions/tasks/${taskId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        throw new Error(`Discussion load failed: ${response.status}`);
      }
      const data = await response.json();
      setDiscussionMessages(data);
      updateStatus("Discussion thread ready.");
    } catch (error) {
      showError(error.message);
    }
  };

  const submitChat = async () => {
    updateStatus("Requesting AI chat response...");
    try {
      const token = tokens.chat || (await requestToken("chat"));
      const response = await fetch(`${baseUrls.chat}/api/chat`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ prompt: chatPrompt }),
      });
      if (!response.ok) {
        throw new Error(`Chat request failed: ${response.status}`);
      }
      const data = await response.json();
      updateStatus(`AI response: ${JSON.stringify(data)}`);
    } catch (error) {
      showError(error.message);
    }
  };

  useEffect(() => {
    if (loggedIn && activeView === "release") {
      loadReleases();
    }
  }, [loggedIn, activeView]);

  useEffect(() => {
    if (loggedIn && activeView === "discussion" && discussionTaskId) {
      loadDiscussionMessages(discussionTaskId);
    }
  }, [loggedIn, activeView, discussionTaskId]);

  const isMenuVisible = loggedIn && activeView === "menu";
  const showFeature = (feature) => loggedIn && activeView === feature;

  const menuItems = [
    { id: "release", label: "Manage releases", roles: ["release-manager", "dev-1", "dev-2"] },
    { id: "discussion", label: "Post discussion", roles: ["release-manager", "dev-1", "dev-2"] },
    { id: "chat", label: "Chat with AI", roles: ["release-manager", "dev-1", "dev-2"] },
    { id: "notifications", label: "Send system alert", roles: ["release-manager", "dev-1", "dev-2"] },
  ];
  const visibleMenuItems = menuItems.filter((item) => item.roles.includes(role));
  const statusLabel = loggedIn ? `Role: ${roleLabel[role] || role} • ${status}` : status;
  const selectedTaskRelease = releases.find((item) => item.id === selectedTaskReleaseId);
  const selectedReleaseTasks =
    selectedTaskRelease && selectedTaskRelease.tasks
      ? [...selectedTaskRelease.tasks].sort((a, b) => a.orderIndex - b.orderIndex)
      : [];
  const selectedTask =
    selectedReleaseTasks.length > 0
      ? selectedReleaseTasks.find((task) => task.id === selectedTaskId)
      : null;
  const selectedReleaseCompletedAt = selectedTaskRelease
    ? selectedTaskRelease.completedAt
      ? new Date(selectedTaskRelease.completedAt).getTime()
      : selectedTaskRelease.lastCompletedAt
      ? new Date(selectedTaskRelease.lastCompletedAt).getTime()
      : null
    : null;

  const buildThreadTree = (messages) => {
    const byParent = new Map();
    messages.forEach((message) => {
      const key = message.parentId || "root";
      if (!byParent.has(key)) {
        byParent.set(key, []);
      }
      byParent.get(key).push(message);
    });
    byParent.forEach((items) =>
      items.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    );

    const buildNodes = (parentId) => {
      const list = byParent.get(parentId || "root") || [];
      return list.map((item) => ({
        ...item,
        children: buildNodes(item.id),
      }));
    };

    return buildNodes("root");
  };

  const threadTree = buildThreadTree(discussionMessages || []);

  const renderThread = (nodes, depth = 0) => (
    <div className={depth === 0 ? "thread" : "thread-children"}>
      {nodes.map((node) => (
        <div key={node.id} className="thread-item">
          <div className="thread-meta">
            <strong>{node.author}</strong>
            <span>•</span>
            <span>{new Date(node.createdAt).toLocaleString()}</span>
          </div>
          <div className="thread-body">{node.message}</div>
          <div className="task-actions">
            <button
              className="secondary"
              onClick={() => {
                setReplyParentId(node.id);
                setDiscussionForm((prev) => ({ ...prev, message: "" }));
              }}
            >
              Reply
            </button>
          </div>
          {node.children && node.children.length > 0 && renderThread(node.children, depth + 1)}
        </div>
      ))}
    </div>
  );

  return (
    <>
      <div className="status-bar">
        <div className="status-bar-inner">
          <div className="status-bar-label">Status</div>
          <div className="status-bar-text">{statusLabel}</div>
        </div>
      </div>

      {!loggedIn && (
        <SectionCard title="Select a role" tone="accent">
          <div className="stack">
            <div>
              <label>Role</label>
              <select
                value={loginForm.role}
                onChange={(event) => setLoginForm({ ...loginForm, role: event.target.value })}
              >
                <option value="release-manager">Release manager</option>
                <option value="dev-1">Developer 1</option>
                <option value="dev-2">Developer 2</option>
              </select>
            </div>
            <div className="inline">
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={loginForm.remember}
                  onChange={(event) =>
                    setLoginForm({ ...loginForm, remember: event.target.checked })
                  }
                />
                Remember this role locally
              </label>
            </div>
            <button onClick={handleLogin}>Start session</button>
          </div>
          <div className="helper">Local demo accounts only. No username or password required.</div>
        </SectionCard>
      )}

      {isMenuVisible && (
        <SectionCard title="Choose a workflow" tone="accent">
          <div className="chip-row">
            <div className="chip">Role: {roleLabel[role] || role}</div>
            <div className="chip">Environment: Localhost</div>
            <div className="chip">Session: Active</div>
          </div>
          <div className="stack">
            {visibleMenuItems.map((item) => (
              <button key={item.id} onClick={() => setActiveView(item.id)}>
                {item.label}
              </button>
            ))}
            <button className="secondary" onClick={handleLogout}>
              Sign out
            </button>
          </div>
          <div className="helper">Tokens are managed in the background. Pick an action to continue.</div>
        </SectionCard>
      )}

      {showFeature("release") && (
        <SectionCard title="Release management">
          <div className="inline">
            <button className="ghost" onClick={() => setActiveView("menu")}>
              Back to menu
            </button>
            <button onClick={() => setIsReleaseModalOpen(true)}>Add release</button>
            <button className="secondary" onClick={handleLogout}>
              Sign out
            </button>
          </div>

          <div className="management-grid">
            <div className="panel">
              <div className="panel-title">Releases</div>
              <div className="list">
                {releases.length === 0 && <div className="list-item">No releases yet.</div>}
                {releases.map((release) => {
                  const isExpanded = expandedReleaseId === release.id;
                  const releaseTasks = release.tasks
                    ? [...release.tasks].sort((a, b) => a.orderIndex - b.orderIndex)
                    : [];
                  const releaseAllCompleted =
                    releaseTasks.length > 0
                      ? releaseTasks.every((task) => task.status === "COMPLETED")
                      : false;

                  return (
                    <div
                      key={release.id}
                      className={`list-item ${release.id === selectedReleaseId ? "active" : ""}`}
                      role="button"
                      tabIndex={0}
                      onClick={() => {
                        setSelectedReleaseId(release.id);
                        setExpandedReleaseId((prev) => (prev === release.id ? "" : release.id));
                      }}
                      onKeyDown={(event) => {
                        if (event.key === "Enter" || event.key === " ") {
                          setSelectedReleaseId(release.id);
                          setExpandedReleaseId((prev) => (prev === release.id ? "" : release.id));
                        }
                      }}
                    >
                      <div className="inline">
                        <div>
                          <strong>{release.name}</strong>
                          <div className="list-meta">Version {release.version}</div>
                        </div>
                        <span className={`badge ${release.completed ? "" : "warning"}`}>
                          {release.completed ? "Completed" : "Active"}
                        </span>
                      </div>
                      <div className="task-actions">
                        <button
                          onClick={(event) => {
                            event.stopPropagation();
                            completeRelease(release.id);
                          }}
                          disabled={release.completed || !releaseAllCompleted}
                        >
                          Complete release
                        </button>
                      </div>

                      {isExpanded && (
                        <div className="list-item-details" onClick={(event) => event.stopPropagation()}>
                          <div className="chip-row">
                            <div className="chip">ID: {release.id}</div>
                            <div className="chip">Status: {release.completed ? "Completed" : "Active"}</div>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="panel">
              <div className="panel-title">Tasks</div>
              {!selectedTaskRelease && <div className="helper">Select a release to view tasks.</div>}
              {selectedTaskRelease && selectedReleaseTasks.length === 0 && (
                <div className="helper">No tasks yet. Add the first task below.</div>
              )}
              {selectedTaskRelease &&
                selectedReleaseTasks.map((task) => {
                  const taskTime = task.createdAt ? new Date(task.createdAt).getTime() : null;
                  const isHotfix =
                    selectedReleaseCompletedAt !== null &&
                    taskTime !== null &&
                    taskTime > selectedReleaseCompletedAt;
                  const isSelected = selectedTaskId === task.id;
                  return (
                    <div
                      key={task.id}
                      className={`task-row ${isSelected ? "selected" : ""}`}
                      onClick={() => {
                        setSelectedTaskReleaseId(selectedTaskRelease.id);
                        setSelectedTaskId(task.id);
                      }}
                    >
                      <div className="inline">
                        <strong>
                          {task.orderIndex}. {task.title}
                        </strong>
                        <span className={`badge ${task.status === "COMPLETED" ? "" : "warning"}`}>
                          {task.status}
                        </span>
                        {selectedTaskRelease && (
                          <span className="list-meta">Release: {selectedTaskRelease.name}</span>
                        )}
                        <span className="list-meta">Assignee: {task.assigneeId}</span>
                        {isHotfix && <span className="badge warning">Hotfix</span>}
                      </div>
                      <div className="list-meta">
                        {task.description || "No description provided."}
                      </div>
                    </div>
                  );
                })}

              <div className="divider"></div>
              <div className="panel-title">Task details</div>
              {selectedTask ? (
                <>
                  <div className="list-meta">
                    {selectedTask.description || "No description provided."}
                  </div>
                  <div className="task-actions">
                    <button
                      className="secondary"
                      disabled={selectedTask.status !== "TODO" || role !== selectedTask.assigneeId}
                      onClick={() => startTask(selectedTaskReleaseId, selectedTask.id)}
                    >
                      Start task
                    </button>
                    <button
                      disabled={
                        selectedTask.status !== "IN_PROCESS" || role !== selectedTask.assigneeId
                      }
                      onClick={() => completeTask(selectedTaskReleaseId, selectedTask.id)}
                    >
                      Complete task
                    </button>
                    <button
                      className="secondary"
                      onClick={() => {
                        setDiscussionTaskId(selectedTask.id);
                        setDiscussionForm((prev) => ({
                          ...prev,
                          releaseId: selectedTaskReleaseId,
                          message: "",
                        }));
                        setReplyParentId("");
                        setActiveView("discussion");
                      }}
                    >
                      Open discussion
                    </button>
                  </div>
                </>
              ) : (
                <div className="helper">Select a task to manage it here.</div>
              )}

              <div className="divider"></div>
              <div className="panel-title">Add task</div>
              {!selectedTaskRelease && (
                <div className="helper">Select a release before adding a task.</div>
              )}
              {selectedTaskRelease && (
                <>
                  <label>Title</label>
                  <input
                    value={taskForm.title}
                    onChange={(event) => setTaskForm({ ...taskForm, title: event.target.value })}
                    placeholder="Implement API endpoint"
                  />
                  <label>Description</label>
                  <textarea
                    rows="2"
                    value={taskForm.description}
                    onChange={(event) =>
                      setTaskForm({ ...taskForm, description: event.target.value })
                    }
                    placeholder="Optional context"
                  ></textarea>
                  <div className="split">
                    <div>
                      <label>Assignee</label>
                      <select
                        value={taskForm.assigneeId}
                        onChange={(event) =>
                          setTaskForm({ ...taskForm, assigneeId: event.target.value })
                        }
                      >
                        <option value="release-manager">Release manager</option>
                        <option value="dev-1">Developer 1</option>
                        <option value="dev-2">Developer 2</option>
                      </select>
                    </div>
                    <div>
                      <label>Order</label>
                      <input
                        type="number"
                        min="1"
                        value={taskForm.orderIndex}
                        onChange={(event) =>
                          setTaskForm({ ...taskForm, orderIndex: event.target.value })
                        }
                      />
                    </div>
                  </div>
                  <button onClick={() => addTask(selectedTaskRelease.id)}>Add task</button>
                  <div className="helper">
                    Adding a task to a completed release reopens it as a hotfix.
                  </div>
                </>
              )}
            </div>
          </div>
        </SectionCard>
      )}

      {showFeature("discussion") && (
        <SectionCard title="Discussion">
          <div className="inline">
            <button className="ghost" onClick={() => setActiveView("menu")}>
              Back to menu
            </button>
            <button className="secondary" onClick={() => setActiveView("release")}>
              Back to releases
            </button>
            <button className="secondary" onClick={handleLogout}>
              Sign out
            </button>
          </div>
          <div className="chip-row">
            <div className="chip">Release: {discussionForm.releaseId || "Not selected"}</div>
            <div className="chip">Task: {discussionTaskId || "Not selected"}</div>
          </div>
          {replyParentId && (
            <div className="status">
              Replying to comment {replyParentId}.{" "}
              <button className="ghost" onClick={() => setReplyParentId("")}>
                Cancel reply
              </button>
            </div>
          )}
          <label>Your message</label>
          <textarea
            rows="3"
            value={discussionForm.message}
            onChange={(event) => setDiscussionForm({ ...discussionForm, message: event.target.value })}
            placeholder="Ask a question or share a solution..."
          ></textarea>
          <button onClick={submitDiscussion}>Post message</button>
          <div className="divider"></div>
          <div className="panel-title">Thread</div>
          {discussionTaskId && discussionMessages.length === 0 && (
            <div className="helper">No messages yet. Start the thread above.</div>
          )}
          {!discussionTaskId && (
            <div className="helper">Select a task from releases to open its discussion.</div>
          )}
          {discussionTaskId && discussionMessages.length > 0 && renderThread(threadTree)}
        </SectionCard>
      )}

      {showFeature("chat") && (
        <SectionCard title="AI chat">
          <div className="inline">
            <button className="ghost" onClick={() => setActiveView("menu")}>
              Back to menu
            </button>
            <button className="secondary" onClick={handleLogout}>
              Sign out
            </button>
          </div>
          <label>Prompt</label>
          <textarea rows="3" value={chatPrompt} onChange={(event) => setChatPrompt(event.target.value)}></textarea>
          <button onClick={submitChat}>Ask AI</button>
        </SectionCard>
      )}

      {showFeature("notifications") && (
        <SectionCard title="Notifications">
          <div className="inline">
            <button className="ghost" onClick={() => setActiveView("menu")}>
              Back to menu
            </button>
            <button className="secondary" onClick={handleLogout}>
              Sign out
            </button>
          </div>
          <div className="status">Notifications are handled in the panel below (Vue workflow).</div>
        </SectionCard>
      )}

      {showFeature("release") && isReleaseModalOpen && (
        <div className="modal-backdrop">
          <div className="modal">
            <div className="panel-title">Create release</div>
            <label>Name</label>
            <input
              value={releaseForm.name}
              onChange={(event) => setReleaseForm({ ...releaseForm, name: event.target.value })}
              placeholder="Release name"
            />
            <label>Version</label>
            <input
              value={releaseForm.version}
              onChange={(event) => setReleaseForm({ ...releaseForm, version: event.target.value })}
              placeholder="1.0.0"
            />
            <div className="split">
              <button className="secondary" onClick={() => setIsReleaseModalOpen(false)}>
                Cancel
              </button>
              <button onClick={submitRelease}>Create release</button>
            </div>
          </div>
        </div>
      )}

      {isErrorOpen && (
        <div className="modal-backdrop" onClick={() => setIsErrorOpen(false)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <div className="panel-title">Action blocked</div>
            <div className="status">{errorMessage}</div>
            <div className="split">
              <button className="secondary" onClick={() => setIsErrorOpen(false)}>
                Close
              </button>
              <button onClick={() => setIsErrorOpen(false)}>Okay</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

ReactDOM.createRoot(document.getElementById("react-root")).render(<App />);
