import "./App.css";

import { useEffect, useMemo, useState } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls } from "@react-three/drei";

function formatMB(bytes) {
  return (bytes / 1024 / 1024).toFixed(2);
}

function formatBytes(bytes) {
  return bytes.toLocaleString();
}

function formatClassName(className) {
  if (!className) {
    return "Unknown";
  }

  if (className === "[B") {
    return "byte[]";
  }

  if (className === "[Ljava.lang.String;") {
    return "String[]";
  }

  if (className === "[Ljava.lang.Object;") {
    return "Object[]";
  }

  return className;
}

/* =========================================================
   3D MEMORY NODE
========================================================= */

function MemoryNode({ position, scale, color }) {
  return (
    <mesh position={position} scale={scale}>
      <sphereGeometry args={[0.12, 12, 12]} />

      <meshStandardMaterial
        color={color}
        emissive={color}
        emissiveIntensity={0.35}
      />
    </mesh>
  );
}

/* =========================================================
   3D MEMORY VISUALIZATION
========================================================= */

function MemoryNodes({ usage, leakDetected }) {
  const nodes = useMemo(() => {
    const result = [];

    for (let i = 0; i < 100; i++) {
      const angle = i * 0.7;
      const radius = 2.5 + (i % 10) * 0.18;

      const x = Math.cos(angle) * radius;
      const y = ((i % 12) - 6) * 0.35;
      const z = Math.sin(angle) * radius;

      const size = 0.7 + usage / 100;

      let color;

      if (leakDetected && i % 5 === 0) {
        color = "#ef4444";
      } else if (i % 10 === 0) {
        color = "#ef4444";
      } else if (i % 3 === 0) {
        color = "#22c55e";
      } else {
        color = "#8b5cf6";
      }

      result.push({
        position: [x, y, z],
        scale: [size, size, size],
        color,
      });
    }

    return result;
  }, [usage, leakDetected]);

  return (
    <>
      {nodes.map((node, index) => (
        <MemoryNode
          key={index}
          position={node.position}
          scale={node.scale}
          color={node.color}
        />
      ))}
    </>
  );
}

/* =========================================================
   HEAP HISTORY
========================================================= */

function HeapHistory({ history }) {
  if (!history.length) {
    return (
      <div className="empty-state">
        Waiting for telemetry...
      </div>
    );
  }

  const values = history.map(
    (item) => item.heapUsed
  );

  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const width = 600;
  const height = 150;

  const points = history
    .map((item, index) => {
      const x =
        history.length === 1
          ? width / 2
          : (index / (history.length - 1)) *
            width;

      const y =
        height -
        ((item.heapUsed - min) / range) *
          height;

      return `${x},${y}`;
    })
    .join(" ");

  return (
    <div>
      <svg
        className="history-svg"
        viewBox={`0 0 ${width} ${height}`}
        preserveAspectRatio="none"
      >
        <polyline
          points={points}
          fill="none"
          stroke="#8b5cf6"
          strokeWidth="3"
        />

        {history.map((item, index) => {
          const x =
            history.length === 1
              ? width / 2
              : (index / (history.length - 1)) *
                width;

          const y =
            height -
            ((item.heapUsed - min) / range) *
              height;

          return (
            <circle
              key={index}
              cx={x}
              cy={y}
              r="3"
              fill="#a78bfa"
            />
          );
        })}
      </svg>

      <div className="chart-labels">
        <span>{formatMB(min)} MB</span>

        <span>{formatMB(max)} MB</span>
      </div>
    </div>
  );
}

/* =========================================================
   TOP MEMORY CONSUMERS
========================================================= */

function TopMemoryConsumers({
  histogram,
  selectedClass,
  onSelectClass,
}) {
  if (!histogram.length) {
    return (
      <div className="empty-state">
        Waiting for heap histogram...
      </div>
    );
  }

  const topClasses = [...histogram]
    .sort(
      (a, b) =>
        b.totalBytes - a.totalBytes
    )
    .slice(0, 8);

  return (
    <div className="consumer-list">
      {topClasses.map((item, index) => {
        const isSelected =
          selectedClass === item.className;

        return (
          <button
            key={`${item.rank}-${item.className}`}
            className={`consumer-row ${
              isSelected
                ? "consumer-row-selected"
                : ""
            }`}
            onClick={() =>
              onSelectClass(item.className)
            }
          >
            <div className="consumer-main">
              <span
                className={
                  index === 0
                    ? "consumer-name top-consumer"
                    : "consumer-name"
                }
                title={item.className}
              >
                {index + 1}.{" "}
                {formatClassName(
                  item.className
                )}
              </span>

              <span className="consumer-memory">
                {formatMB(item.totalBytes)} MB
              </span>
            </div>

            <div className="consumer-meta">
              <span>
                {item.instanceCount.toLocaleString()}{" "}
                objects
              </span>

              <span>
                {formatBytes(item.totalBytes)} bytes
              </span>
            </div>
          </button>
        );
      })}
    </div>
  );
}

/* =========================================================
   LEAK DETECTION
========================================================= */

function LeakDetectionPanel({
  leakCandidates,
  onSelectClass,
}) {
  if (!leakCandidates.length) {
    return (
      <div className="empty-state leak-safe">
        <div className="safe-icon">✓</div>

        <div>
          No persistent memory growth detected
        </div>

        <div className="small-muted">
          Analyzing recent JVM snapshots
        </div>
      </div>
    );
  }

  return (
    <div>
      {leakCandidates.map((item, index) => (
        <button
          key={item.className}
          className="leak-row"
          onClick={() =>
            onSelectClass(item.className)
          }
        >
          <div className="leak-row-top">
            <span
              className="leak-class"
              title={item.className}
            >
              {index + 1}.{" "}
              {formatClassName(
                item.className
              )}
            </span>

            <span className="leak-growth">
              +{item.totalGrowthMB.toFixed(2)} MB
            </span>
          </div>

          <div className="leak-row-bottom">
            <span>
              {item.instanceCount.toLocaleString()}{" "}
              objects
            </span>

            <span>
              {item.growthStreak} growth samples
            </span>
          </div>
        </button>
      ))}

      <p className="warning-text">
        ⚠️ Repeated growth indicates a potential
        leak; it does not by itself prove a leak.
      </p>
    </div>
  );
}

/* =========================================================
   HEAP SNAPSHOT PANEL
========================================================= */

function HeapSnapshotPanel({
  snapshot,
  snapshotLoading,
  onCreateSnapshot,

  matAnalysis,
  matLoading,
  onAnalyzeWithMat,
  onOpenMatReport,
}) {
  return (
    <section className="panel snapshot-panel">
      <div className="panel-title">
        <h2>📸 Heap Snapshot</h2>

        <span
          className={
            snapshot?.success
              ? "badge-safe"
              : "panel-hint"
          }
        >
          {snapshot?.success
            ? "READY"
            : "JVM"}
        </span>
      </div>

      <p className="snapshot-description">
        Create a real JVM heap dump for deeper
        object and reference analysis.
      </p>

      <button
        className="snapshot-button"
        onClick={onCreateSnapshot}
        disabled={snapshotLoading}
      >
        {snapshotLoading
          ? "⏳ Creating Snapshot..."
          : "📸 Create Heap Snapshot"}
      </button>

      {snapshot?.success && (
        <div className="snapshot-result">
          <div className="snapshot-status">
            <span className="snapshot-success-icon">
              ✓
            </span>

            <strong>
              Snapshot Created
            </strong>
          </div>

          <div className="snapshot-info">
            <div>
              <span>File</span>

              <strong title={snapshot.fileName}>
                {snapshot.fileName}
              </strong>
            </div>

            <div>
              <span>Size</span>

              <strong>
                {snapshot.sizeMB} MB
              </strong>
            </div>

            <div>
              <span>JVM PID</span>

              <strong>
                {snapshot.pid}
              </strong>
            </div>
          </div>
        </div>
      )}

      {snapshot &&
        snapshot.success === false && (
          <div className="snapshot-error">
            <strong>
              ⚠ Snapshot creation failed
            </strong>

            <p>
              {snapshot.message ||
                "Unable to create heap dump."}
            </p>
          </div>
        )}

      {/* =====================================================
          MAT ANALYSIS
      ===================================================== */}

      <div
        style={{
          marginTop: "14px",
          paddingTop: "14px",
          borderTop:
            "1px solid rgba(148, 163, 184, 0.12)",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: "10px",
            marginBottom: "8px",
          }}
        >
          <strong
            style={{
              color: "#e2e8f0",
              fontSize: "12px",
            }}
          >
            🔬 Eclipse MAT Analysis
          </strong>

          <span
            className={
              matAnalysis?.success
                ? "badge-safe"
                : "panel-hint"
            }
          >
            {matLoading
              ? "ANALYZING"
              : matAnalysis?.success
                ? "COMPLETE"
                : "READY"}
          </span>
        </div>

        <p className="snapshot-description">
          Run Eclipse Memory Analyzer against the
          latest JVM heap dump and generate a leak
          suspects report.
        </p>

        <button
          className="snapshot-button"
          onClick={onAnalyzeWithMat}
          disabled={matLoading}
        >
          {matLoading
            ? "⏳ Analyzing Heap Dump..."
            : "🔬 Analyze with MAT"}
        </button>

        {matAnalysis?.success && (
          <div className="snapshot-result">
            <div className="snapshot-status">
              <span className="snapshot-success-icon">
                ✓
              </span>

              <strong>
                MAT Analysis Complete
              </strong>
            </div>

            <div className="snapshot-info">
              <div>
                <span>Heap Dump</span>

                <strong
                  title={matAnalysis.heapDump}
                >
                  {matAnalysis.heapDump}
                </strong>
              </div>

              <div>
                <span>Report</span>

                <strong
                  title={matAnalysis.reportZip}
                >
                  {matAnalysis.reportZip}
                </strong>
              </div>

              <div>
                <span>MAT Exit Code</span>

                <strong>
                  {matAnalysis.exitCode ?? 0}
                </strong>
              </div>
            </div>

            {matAnalysis.reportUrl && (
              <button
                className="snapshot-button"
                style={{
                  marginTop: "11px",
                  background:
                    "linear-gradient(135deg, #22c55e, #16a34a)",
                }}
                onClick={onOpenMatReport}
              >
                📊 Open Full MAT Report
              </button>
            )}
          </div>
        )}

        {matAnalysis &&
          matAnalysis.success === false && (
            <div className="snapshot-error">
              <strong>
                ⚠ MAT analysis failed
              </strong>

              <p>
                {matAnalysis.message ||
                  "Unable to analyze the heap dump."}
              </p>

              {matAnalysis.error && (
                <p>
                  {matAnalysis.error}
                </p>
              )}
            </div>
          )}
      </div>

      <p className="panel-note">
        Heap dumps can temporarily pause the JVM.
      </p>
    </section>
  );
}

/* =========================================================
   CLASS DETAILS
========================================================= */

function ClassDetails({
  selectedClass,
  histogram,
  histogramHistory,
  leakCandidates,
  onClose,
}) {
  if (!selectedClass) {
    return null;
  }

  const current = histogram.find(
    (item) =>
      item.className === selectedClass
  );

  if (!current) {
    return null;
  }

  const sortedHistogram = [...histogram].sort(
    (a, b) =>
      b.totalBytes - a.totalBytes
  );

  const rank =
    sortedHistogram.findIndex(
      (item) =>
        item.className === selectedClass
    ) + 1;

  const snapshots = histogramHistory.map(
    (snapshot) => {
      const item = snapshot.data.find(
        (entry) =>
          entry.className === selectedClass
      );

      return {
        timestamp: snapshot.timestamp,
        totalBytes: item
          ? item.totalBytes
          : 0,
        instanceCount: item
          ? item.instanceCount
          : 0,
      };
    }
  );

  const validSnapshots = snapshots.filter(
    (item) => item.totalBytes > 0
  );

  const firstValue = validSnapshots.length
    ? validSnapshots[0].totalBytes
    : current.totalBytes;

  const growthBytes =
    current.totalBytes - firstValue;

  const growthMB =
    growthBytes / 1024 / 1024;

  const growthPercent =
    firstValue > 0
      ? (growthBytes / firstValue) * 100
      : 0;

  const averageGrowthMB =
    validSnapshots.length > 1
      ? growthMB / (validSnapshots.length - 1)
      : 0;

  const leakCandidate =
    leakCandidates.find(
      (item) =>
        item.className === selectedClass
    );

  const historyValues =
    validSnapshots.map(
      (item) => item.totalBytes
    );

  const maxHistory = Math.max(
    ...historyValues,
    1
  );

  return (
    <div className="modal-backdrop">
      <div className="class-details-modal">
        <div className="modal-header">
          <div>
            <div className="modal-title">
              Class Details
            </div>

            <div className="selected-class-name">
              {formatClassName(selectedClass)}
            </div>
          </div>

          <button
            className="close-button"
            onClick={onClose}
          >
            ✕
          </button>
        </div>

        <div className="details-grid">
          <div className="detail-card">
            <span>RANK</span>

            <strong>#{rank}</strong>
          </div>

          <div className="detail-card">
            <span>OBJECTS</span>

            <strong>
              {current.instanceCount.toLocaleString()}
            </strong>
          </div>

          <div className="detail-card">
            <span>MEMORY</span>

            <strong>
              {formatMB(current.totalBytes)} MB
            </strong>
          </div>

          <div
            className={`detail-card ${
              growthBytes > 0
                ? "growth-card"
                : "stable-card"
            }`}
          >
            <span>GROWTH</span>

            <strong>
              {growthBytes >= 0 ? "+" : ""}
              {growthMB.toFixed(2)} MB
            </strong>

            <small>
              {growthPercent.toFixed(1)}%
            </small>
          </div>
        </div>

        <div
          className={
            leakCandidate
              ? "analysis-status leak-status"
              : "analysis-status normal-status"
          }
        >
          <div>
            <strong>
              {leakCandidate
                ? "🚨 Potential Leak Detected"
                : "🛡 No Leak Pattern Detected"}
            </strong>

            <div className="status-description">
              {leakCandidate
                ? `Repeated growth across ${leakCandidate.growthStreak} samples.`
                : "Current snapshots do not meet the configured leak-growth threshold."}
            </div>
          </div>
        </div>

        <div className="analysis-section">
          <div className="section-heading">
            <h3>Memory Growth History</h3>

            <span>
              {validSnapshots.length} snapshots
            </span>
          </div>

          {validSnapshots.length < 2 ? (
            <div className="chart-empty">
              Collecting more snapshots...
            </div>
          ) : (
            <div className="detail-chart">
              {historyValues.map(
                (value, index) => {
                  const height = Math.max(
                    6,
                    (value / maxHistory) * 130
                  );

                  return (
                    <div
                      key={index}
                      className={`chart-bar ${
                        index ===
                        historyValues.length - 1
                          ? "chart-bar-current"
                          : ""
                      }`}
                      style={{
                        height: `${height}px`,
                      }}
                      title={`${formatMB(
                        value
                      )} MB`}
                    />
                  );
                }
              )}
            </div>
          )}
        </div>

        <div className="analysis-section">
          <div className="section-heading">
            <h3>Growth Analysis</h3>
          </div>

          <div className="analysis-grid">
            <div>
              <span>Average growth</span>

              <strong>
                {averageGrowthMB.toFixed(2)} MB/sample
              </strong>
            </div>

            <div>
              <span>Current objects</span>

              <strong>
                {current.instanceCount.toLocaleString()}
              </strong>
            </div>

            <div>
              <span>Current memory</span>

              <strong>
                {formatMB(current.totalBytes)} MB
              </strong>
            </div>

            <div>
              <span>Growth samples</span>

              <strong>
                {leakCandidate
                  ? leakCandidate.growthStreak
                  : 0}
              </strong>
            </div>
          </div>
        </div>

        <div className="analysis-section">
          <div className="section-heading">
            <h3>Recent Snapshots</h3>
          </div>

          <div className="snapshot-table">
            <div className="snapshot-header">
              <span>Snapshot</span>
              <span>Objects</span>
              <span>Memory</span>
            </div>

            {validSnapshots
              .slice(-5)
              .reverse()
              .map((snapshot, index) => (
                <div
                  className="snapshot-row"
                  key={`${snapshot.timestamp}-${index}`}
                >
                  <span>#{index + 1}</span>

                  <span>
                    {snapshot.instanceCount.toLocaleString()}
                  </span>

                  <span>
                    {formatMB(
                      snapshot.totalBytes
                    )}{" "}
                    MB
                  </span>
                </div>
              ))}
          </div>
        </div>

        <div className="modal-footer">
          Data source: JVM GC.class_histogram
        </div>
      </div>
    </div>
  );
}

/* =========================================================
   APP
========================================================= */

function App() {
  const [memory, setMemory] = useState(null);

  const [history, setHistory] = useState([]);

  const [classStats, setClassStats] =
    useState(null);

  const [histogram, setHistogram] =
    useState([]);

  const [
    histogramHistory,
    setHistogramHistory,
  ] = useState([]);

  const [
    leakCandidates,
    setLeakCandidates,
  ] = useState([]);

  const [
    selectedClass,
    setSelectedClass,
  ] = useState(null);

  const [
    leakStatus,
    setLeakStatus,
  ] = useState("stopped");

  const [
    retainedMB,
    setRetainedMB,
  ] = useState(0);

  const [
    leakLoading,
    setLeakLoading,
  ] = useState(false);

  const [
    snapshot,
    setSnapshot,
  ] = useState(null);

  const [
    snapshotLoading,
    setSnapshotLoading,
  ] = useState(false);

  /* =======================================================
     MAT ANALYSIS STATE
  ======================================================= */

  const [
    matAnalysis,
    setMatAnalysis,
  ] = useState(null);

  const [
    matLoading,
    setMatLoading,
  ] = useState(false);

  /* =======================================================
     WEBSOCKET
  ======================================================= */

  useEffect(() => {
    const socket = new WebSocket(
      "ws://localhost:8080/ws/telemetry"
    );

    socket.onopen = () => {
      console.log(
        "Connected to HeapVortex WebSocket"
      );
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);

        setMemory(data);
      } catch (error) {
        console.error(
          "WebSocket JSON error:",
          error
        );
      }
    };

    socket.onerror = (error) => {
      console.error(
        "WebSocket error:",
        error
      );
    };

    socket.onclose = () => {
      console.log(
        "HeapVortex WebSocket disconnected"
      );
    };

    return () => socket.close();
  }, []);

  /* =======================================================
     HEAP HISTORY
  ======================================================= */

  useEffect(() => {
    const loadHistory = async () => {
      try {
        const response = await fetch(
          "http://localhost:8080/api/telemetry/history"
        );

        if (!response.ok) {
          throw new Error(
            "Telemetry history request failed"
          );
        }

        const data = await response.json();

        setHistory(data.slice(-60));
      } catch (error) {
        console.error(
          "History error:",
          error
        );
      }
    };

    loadHistory();

    const interval = setInterval(
      loadHistory,
      2000
    );

    return () =>
      clearInterval(interval);
  }, []);

  /* =======================================================
     CLASS STATISTICS
  ======================================================= */

  useEffect(() => {
    const loadClassStats = async () => {
      try {
        const response = await fetch(
          "http://localhost:8080/api/jvm/classes"
        );

        if (!response.ok) {
          throw new Error(
            "Class statistics request failed"
          );
        }

        const data = await response.json();

        setClassStats(data);
      } catch (error) {
        console.error(
          "Class statistics error:",
          error
        );
      }
    };

    loadClassStats();

    const interval = setInterval(
      loadClassStats,
      3000
    );

    return () =>
      clearInterval(interval);
  }, []);

  /* =======================================================
     HEAP HISTOGRAM
  ======================================================= */

  useEffect(() => {
    const loadHistogram = async () => {
      try {
        const response = await fetch(
          "http://localhost:8080/api/jvm/histogram"
        );

        if (!response.ok) {
          throw new Error(
            "Heap histogram request failed"
          );
        }

        const data = await response.json();

        setHistogram(data);

        setHistogramHistory(
          (previous) => {
            const next = [
              ...previous,
              {
                timestamp: Date.now(),
                data,
              },
            ];

            return next.slice(-8);
          }
        );
      } catch (error) {
        console.error(
          "Histogram error:",
          error
        );
      }
    };

    loadHistogram();

    const interval = setInterval(
      loadHistogram,
      5000
    );

    return () =>
      clearInterval(interval);
  }, []);

  /* =======================================================
     LEAK DETECTION
  ======================================================= */

  useEffect(() => {
    if (histogramHistory.length < 3) {
      setLeakCandidates([]);

      return;
    }

    const classSnapshots = new Map();

    histogramHistory.forEach(
      (snapshot) => {
        snapshot.data.forEach(
          (item) => {
            if (
              !classSnapshots.has(
                item.className
              )
            ) {
              classSnapshots.set(
                item.className,
                []
              );
            }

            classSnapshots
              .get(item.className)
              .push(item);
          }
        );
      }
    );

    const candidates = [];

    classSnapshots.forEach(
      (items, className) => {
        if (items.length < 3) {
          return;
        }

        let growthStreak = 0;
        let totalGrowthBytes = 0;

        for (
          let i = 1;
          i < items.length;
          i++
        ) {
          const growth =
            items[i].totalBytes -
            items[i - 1].totalBytes;

          if (growth > 0) {
            growthStreak++;
            totalGrowthBytes += growth;
          } else {
            growthStreak = 0;
          }
        }

        const totalGrowthMB =
          totalGrowthBytes /
          1024 /
          1024;

        const latest =
          items[items.length - 1];

        if (
          growthStreak >= 3 &&
          totalGrowthMB >= 0.5
        ) {
          candidates.push({
            className,
            instanceCount:
              latest.instanceCount,
            totalBytes:
              latest.totalBytes,
            totalGrowthMB,
            growthStreak,
          });
        }
      }
    );

    candidates.sort(
      (a, b) =>
        b.totalGrowthMB -
        a.totalGrowthMB
    );

    setLeakCandidates(
      candidates.slice(0, 8)
    );
  }, [histogramHistory]);

  /* =======================================================
     LEAK SIMULATOR STATUS
  ======================================================= */

  useEffect(() => {
    const loadLeakStatus = async () => {
      try {
        const response = await fetch(
          "http://localhost:8080/api/leak/status"
        );

        if (!response.ok) {
          throw new Error(
            "Leak status request failed"
          );
        }

        const data = await response.json();

        setLeakStatus(data.status);
        setRetainedMB(data.retainedMB);
      } catch (error) {
        console.error(
          "Leak status error:",
          error
        );
      }
    };

    loadLeakStatus();

    const interval = setInterval(
      loadLeakStatus,
      2000
    );

    return () =>
      clearInterval(interval);
  }, []);

  /* =======================================================
     LEAK CONTROLS
  ======================================================= */

  const controlLeak = async (action) => {
    setLeakLoading(true);

    try {
      const response = await fetch(
        `http://localhost:8080/api/leak/${action}`,
        {
          method: "POST",
        }
      );

      if (!response.ok) {
        throw new Error(
          `Leak ${action} request failed`
        );
      }

      const data = await response.json();

      setLeakStatus(data.status);
      setRetainedMB(data.retainedMB);
    } catch (error) {
      console.error(
        `Leak ${action} error:`,
        error
      );
    } finally {
      setLeakLoading(false);
    }
  };

  /* =======================================================
     CREATE HEAP SNAPSHOT
  ======================================================= */

  const createHeapSnapshot = async () => {
    if (snapshotLoading) {
      return;
    }

    setSnapshotLoading(true);

    setSnapshot(null);

    // Clear previous MAT result because
    // this is a new heap snapshot.
    setMatAnalysis(null);

    try {
      const response = await fetch(
        "http://localhost:8080/api/jvm/heap-dump",
        {
          method: "POST",
        }
      );

      if (!response.ok) {
        throw new Error(
          "Heap snapshot request failed"
        );
      }

      const data = await response.json();

      setSnapshot({
        ...data,
        createdAt: new Date(),
      });
    } catch (error) {
      console.error(
        "Heap snapshot error:",
        error
      );

      setSnapshot({
        success: false,
        message:
          error.message ||
          "Unable to create heap snapshot.",
      });
    } finally {
      setSnapshotLoading(false);
    }
  };

  /* =======================================================
     ANALYZE WITH ECLIPSE MAT
  ======================================================= */

  const analyzeWithMat = async () => {
    if (matLoading) {
      return;
    }

    setMatLoading(true);

    setMatAnalysis(null);

    try {
      const response = await fetch(
        "http://localhost:8080/api/jvm/mat-analyze",
        {
          method: "POST",
        }
      );

      if (!response.ok) {
        throw new Error(
          "MAT analysis request failed"
        );
      }

      const data = await response.json();

      setMatAnalysis({
        ...data,
        analyzedAt: new Date(),
      });
    } catch (error) {
      console.error(
        "MAT analysis error:",
        error
      );

      setMatAnalysis({
        success: false,
        message:
          error.message ||
          "Unable to analyze heap dump with Eclipse MAT.",
      });
    } finally {
      setMatLoading(false);
    }
  };

  /* =======================================================
     OPEN MAT REPORT
  ======================================================= */

  const openMatReport = () => {
    if (!matAnalysis?.reportUrl) {
      return;
    }

    const reportUrl =
      `http://localhost:8080${matAnalysis.reportUrl}`;

    window.open(
      reportUrl,
      "_blank",
      "noopener,noreferrer"
    );
  };

  /* =======================================================
     CALCULATED VALUES
  ======================================================= */

  const usage =
    memory?.heapUsagePercentage ?? 0;

  const heapUsedMB = memory
    ? memory.heapUsed / 1024 / 1024
    : 0;

  const heapMaxMB = memory
    ? memory.heapMax / 1024 / 1024
    : 0;

  const isRunning =
    leakStatus === "running";

  const leakDetected =
    leakCandidates.length > 0;

  /* =======================================================
     UI
  ======================================================= */

  return (
    <div className="app">

      {/* HEADER */}

      <header className="header">
        <div>
          <h1>HeapVortex</h1>

          <p>
            3D JVM Memory Leak Profiler
          </p>
        </div>
      </header>

      {/* DASHBOARD */}

      <main className="dashboard">

        {/* LEFT COLUMN */}

        <aside className="left-column">

          {/* LEAK SIMULATOR */}

          <section className="panel">
            <div className="panel-title">
              <h2>
                Memory Leak Simulator
              </h2>
            </div>

            <p className="status-line">
              <strong>Status:</strong>{" "}
              <span
                className={
                  isRunning
                    ? "status-running"
                    : "status-stopped"
                }
              >
                {leakStatus.toUpperCase()}
              </span>
            </p>

            <p className="retained">
              <strong>Retained:</strong>{" "}
              {retainedMB} MB
            </p>

            <div className="button-stack">

              <button
                className="action-button start-button"
                onClick={() =>
                  controlLeak("start")
                }
                disabled={
                  leakLoading ||
                  isRunning
                }
              >
                ▶ Start Leak
              </button>

              <button
                className="action-button stop-button"
                onClick={() =>
                  controlLeak("stop")
                }
                disabled={
                  leakLoading ||
                  !isRunning
                }
              >
                ⏹ Stop Leak
              </button>

              <button
                className="action-button clear-button"
                onClick={() =>
                  controlLeak("clear")
                }
                disabled={leakLoading}
              >
                🗑 Clear Memory
              </button>

            </div>
          </section>

          {/* JVM CLASS STATISTICS */}

          <section className="panel">
            <div className="panel-title">
              <h2>
                JVM Class Statistics
              </h2>
            </div>

            <div className="stat-list">

              <div className="stat-row">
                <span>
                  Loaded Classes
                </span>

                <strong>
                  {classStats
                    ? classStats.loadedClassCount
                    : "Loading..."}
                </strong>
              </div>

              <div className="stat-row">
                <span>
                  Total Loaded
                </span>

                <strong>
                  {classStats
                    ? classStats.totalLoadedClassCount
                    : "Loading..."}
                </strong>
              </div>

              <div className="stat-row">
                <span>
                  Unloaded Classes
                </span>

                <strong>
                  {classStats
                    ? classStats.unloadedClassCount
                    : "Loading..."}
                </strong>
              </div>

            </div>

            <p className="panel-note">
              Live JVM class-loading data
            </p>
          </section>

          {/* HEAP SNAPSHOT */}

          <HeapSnapshotPanel
            snapshot={snapshot}
            snapshotLoading={
              snapshotLoading
            }
            onCreateSnapshot={
              createHeapSnapshot
            }
            matAnalysis={
              matAnalysis
            }
            matLoading={
              matLoading
            }
            onAnalyzeWithMat={
              analyzeWithMat
            }
            onOpenMatReport={
              openMatReport
            }
          />

          {/* LEAK DETECTION */}

          <section
            className={`panel ${
              leakDetected
                ? "panel-danger"
                : "panel-safe"
            }`}
          >
            <div className="panel-title">
              <h2>
                {leakDetected
                  ? "🚨 Leak Detection"
                  : "🛡 Leak Detection"}
              </h2>

              <span
                className={
                  leakDetected
                    ? "badge-danger"
                    : "badge-safe"
                }
              >
                {leakDetected
                  ? "ALERT"
                  : "MONITORING"}
              </span>
            </div>

            <LeakDetectionPanel
              leakCandidates={
                leakCandidates
              }
              onSelectClass={
                setSelectedClass
              }
            />
          </section>

        </aside>

        {/* CENTER */}

        <section className="center-column">

          <div className="visualization-panel">

            <div className="visualization-header">

              <div>
                <h2>
                  JVM Memory Visualization
                </h2>

                <p>
                  Live object-space representation
                </p>
              </div>

              <div className="live-indicator">
                <span className="live-dot" />
                LIVE
              </div>

            </div>

            <div className="canvas-container">

              <Canvas
                camera={{
                  position: [8, 5, 8],
                  fov: 60,
                }}
              >

                <ambientLight
                  intensity={0.6}
                />

                <directionalLight
                  position={[5, 8, 5]}
                  intensity={2}
                />

                <pointLight
                  position={[-5, 3, -5]}
                  intensity={1}
                />

                <MemoryNodes
                  usage={usage}
                  leakDetected={
                    leakDetected
                  }
                />

                <OrbitControls
                  enableDamping
                  dampingFactor={0.05}
                />

              </Canvas>

            </div>

          </div>

        </section>

        {/* RIGHT COLUMN */}

        <aside className="right-column">

          {/* JVM MEMORY */}

          <section className="panel">

            <div className="panel-title">
              <h2>JVM Memory</h2>
            </div>

            <div className="memory-values">

              <div>
                <span>Heap Used</span>

                <strong>
                  {memory
                    ? `${heapUsedMB.toFixed(
                        2
                      )} MB`
                    : "Connecting..."}
                </strong>
              </div>

              <div>
                <span>Heap Max</span>

                <strong>
                  {memory
                    ? `${heapMaxMB.toFixed(
                        2
                      )} MB`
                    : "Connecting..."}
                </strong>
              </div>

              <div>
                <span>Usage</span>

                <strong>
                  {memory
                    ? `${usage.toFixed(
                        2
                      )}%`
                    : "Connecting..."}
                </strong>
              </div>

            </div>

            <div className="progress-track">

              <div
                className="progress-bar"
                style={{
                  width: `${Math.min(
                    usage,
                    100
                  )}%`,
                  background:
                    usage > 80
                      ? "#ef4444"
                      : usage > 50
                      ? "#f59e0b"
                      : "#22c55e",
                }}
              />

            </div>

            <p className="panel-note">
              Live WebSocket telemetry
            </p>

          </section>

          {/* TOP MEMORY CONSUMERS */}

          <section className="panel consumers-panel">

            <div className="panel-title">

              <h2>
                Top Memory Consumers
              </h2>

              <span className="panel-hint">
                CLICK A CLASS
              </span>

            </div>

            <TopMemoryConsumers
              histogram={histogram}
              selectedClass={
                selectedClass
              }
              onSelectClass={
                setSelectedClass
              }
            />

          </section>

        </aside>

        {/* BOTTOM HISTORY */}

        <section className="bottom-history panel">

          <div className="panel-title">

            <h2>
              Heap Usage History
            </h2>

          </div>

          <HeapHistory
            history={history}
          />

        </section>

      </main>

      {/* CLASS DETAILS */}

      <ClassDetails
        selectedClass={
          selectedClass
        }
        histogram={histogram}
        histogramHistory={
          histogramHistory
        }
        leakCandidates={
          leakCandidates
        }
        onClose={() =>
          setSelectedClass(null)
        }
      />

    </div>
  );
}

export default App;