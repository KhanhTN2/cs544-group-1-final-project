const { useEffect, useState } = React;

const baseUrls = {
  releases: "http://localhost:8081",
  discussions: "http://localhost:8082",
  chat: "http://localhost:8083",
  auth: "http://localhost:8086",
};

const SectionCard = ({ title, tone, children }) => (
  <div className={`card ${tone || ""}`.trim()}>
    <h2>{title}</h2>
    <div className="stack">{children}</div>
  </div>
);

const App = () => {
  const [loginForm, setLoginForm] = useState({
    userId: "admin",
    remember: true,
  });
  const [userId, setUserId] = useState("admin");
  const [userRole, setUserRole] = useState("ADMIN");
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
    assigneeId: "admin",
    orderIndex: 1,
  });
  const [discussionForm, setDiscussionForm] = useState({
    releaseId: "",
    author: "admin",
    message: "",
  });
  const [discussionTaskId, setDiscussionTaskId] = useState("");
  const [discussionMessages, setDiscussionMessages] = useState([]);
  const [replyParentId, setReplyParentId] = useState("");
  const [chatPrompt, setChatPrompt] = useState("Summarize");
  const [status, setStatus] = useState("Choose a user to start your local session.");
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
    admin: "Admin",
    "dev-1": "Developer 1",
    "dev-2": "Developer 2",
  };
  const roleByUser = {
    admin: "ADMIN",
    "dev-1": "DEVELOPER",
    "dev-2": "DEVELOPER",
  };

  const handleLogin = () => {
    const selectedUserId = loginForm.userId;
    if (!selectedUserId) {
      updateStatus("Select a user to continue.");
      return;
    }
    setUserId(selectedUserId);
    setUserRole(roleByUser[selectedUserId] || "DEVELOPER");
    setLoggedIn(true);
    setActiveView("menu");
    updateStatus(`Signed in locally as ${roleLabel[selectedUserId] || selectedUserId}.`);
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
    document.body.dataset.role = userRole;
    document.body.dataset.userId = userId;
  }, [activeView, userRole, userId]);

  useEffect(() => {
    setTaskForm((prev) => ({ ...prev, assigneeId: userId }));
    setDiscussionForm((prev) => ({ ...prev, author: userId }));
  }, [userId]);

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

  const requestAuthToken = async () => {
    if (tokens.auth) {
      return tokens.auth;
    }
    updateStatus("Requesting auth token...");
    try {
      const loginResponse = await fetch(`${baseUrls.auth}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: userId, password: "secret" }),
      });

      if (loginResponse.ok) {
        const data = await loginResponse.json();
        const token = data.token || data.accessToken || "";
        setTokens((prev) => ({ ...prev, auth: token }));
        updateStatus("Auth token ready.");
        return token;
      }

      if (loginResponse.status !== 400) {
        throw new Error(`Auth login failed: ${loginResponse.status}`);
      }

      const registerResponse = await fetch(`${baseUrls.auth}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: userId, password: "secret", role: userRole }),
      });
      if (!registerResponse.ok) {
        throw new Error(`Auth register failed: ${registerResponse.status}`);
      }

      const retryLogin = await fetch(`${baseUrls.auth}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: userId, password: "secret" }),
      });
      if (!retryLogin.ok) {
        throw new Error(`Auth login failed: ${retryLogin.status}`);
      }
      const data = await retryLogin.json();
      const token = data.token || data.accessToken || "";
      setTokens((prev) => ({ ...prev, auth: token }));
      updateStatus("Auth token ready.");
      return token;
    } catch (error) {
      showError(error.message);
      throw error;
    }
  };

  const getJwt = async () => tokens.auth || (await requestAuthToken());

  const loadReleases = async (selectFirst = true) => {
    updateStatus("Loading releases...");
    try {
      const token = await getJwt();
      const response = await fetch(`${baseUrls.releases}/releases`, {
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
      const token = await getJwt();
      const response = await fetch(`${baseUrls.releases}/releases`, {
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
      const token = await getJwt();
      const payload = {
        title: taskForm.title,
        description: taskForm.description,
        assigneeId: taskForm.assigneeId,
        orderIndex: Number(taskForm.orderIndex),
      };
      const response = await fetch(`${baseUrls.releases}/releases/${targetReleaseId}/tasks`, {
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

  const startTask = async (taskId) => {
    updateStatus("Starting task...");
    try {
      const token = await getJwt();
      const response = await fetch(`${baseUrls.releases}/tasks/${taskId}/start`, {
        method: "PATCH",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
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

  const completeTask = async (taskId) => {
    updateStatus("Completing task...");
    try {
      const token = await getJwt();
      const response = await fetch(`${baseUrls.releases}/tasks/${taskId}/complete`, {
        method: "PATCH",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
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
      const token = await getJwt();
      const response = await fetch(`${baseUrls.releases}/releases/${targetReleaseId}/complete`, {
        method: "PATCH",
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
      const token = await getJwt();
      const isReply = Boolean(replyParentId);
      const endpoint = isReply
        ? `${baseUrls.discussions}/comments/${replyParentId}/reply`
        : `${baseUrls.discussions}/tasks/${discussionTaskId}/comments`;
      const payload = isReply
        ? {
            releaseId: discussionForm.releaseId,
            taskId: discussionTaskId,
            author: discussionForm.author,
            message: discussionForm.message,
          }
        : {
            releaseId: discussionForm.releaseId,
            author: discussionForm.author,
            message: discussionForm.message,
          };
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
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
      const token = await getJwt();
      const response = await fetch(`${baseUrls.discussions}/tasks/${taskId}/comments`, {
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
      const token = await getJwt();
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
    { id: "release", label: "Manage releases", roles: ["ADMIN", "DEVELOPER"] },
    { id: "discussion", label: "Post discussion", roles: ["ADMIN", "DEVELOPER"] },
    { id: "chat", label: "Chat with AI", roles: ["ADMIN", "DEVELOPER"] },
    { id: "notifications", label: "Send system alert", roles: ["ADMIN", "DEVELOPER"] },
    { id: "email", label: "Open inbox", roles: ["ADMIN", "DEVELOPER"] },
  ];
  const visibleMenuItems = menuItems.filter((item) => item.roles.includes(userRole));
  const statusLabel = loggedIn ? `User: ${roleLabel[userId] || userId} • ${status}` : status;
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
        <SectionCard title="Select a user" tone="accent">
          <div className="stack">
            <div>
              <label>User</label>
              <select
                value={loginForm.userId}
                onChange={(event) => setLoginForm({ ...loginForm, userId: event.target.value })}
              >
                <option value="admin">Admin</option>
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
                Remember this user locally
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
            <div className="chip">User: {roleLabel[userId] || userId}</div>
            <div className="chip">Environment: Localhost</div>
            <div className="chip">Session: Active</div>
          </div>
          <div className="stack">
            {visibleMenuItems.map((item) => (
              <button
                key={item.id}
                onClick={() => {
                  if (item.id === "email") {
                    window.open("http://localhost:8025", "_blank", "noopener,noreferrer");
                    updateStatus("Opened Mailhog inbox.");
                    return;
                  }
                  setActiveView(item.id);
                }}
              >
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
            {userRole === "ADMIN" && (
              <button onClick={() => setIsReleaseModalOpen(true)}>Add release</button>
            )}
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
                      {userRole === "ADMIN" && !release.completed && releaseAllCompleted && (
                        <div className="task-actions">
                          <button
                            onClick={(event) => {
                              event.stopPropagation();
                              completeRelease(release.id);
                            }}
                          >
                            Complete release
                          </button>
                        </div>
                      )}

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
                      disabled={selectedTask.status !== "TODO" || userId !== selectedTask.assigneeId}
                      onClick={() => startTask(selectedTask.id)}
                    >
                      Start task
                    </button>
                    <button
                      disabled={
                        selectedTask.status !== "IN_PROCESS" || userId !== selectedTask.assigneeId
                      }
                      onClick={() => completeTask(selectedTask.id)}
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
              {selectedTaskRelease && userRole === "ADMIN" && (
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
                        <option value="admin">Admin</option>
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
              {selectedTaskRelease && userRole !== "ADMIN" && (
                <div className="helper">Only admins can add tasks.</div>
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
