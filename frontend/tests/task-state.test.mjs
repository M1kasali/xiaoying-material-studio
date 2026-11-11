import { test } from "node:test";
import assert from "node:assert/strict";
import { active, fileError, mergeTask, pollDelay } from "../src/lib/task-state.ts";

const task = (id, status, time = 2) => ({ id, status, updatedAt: `2026-11-11T00:00:0${time}Z` });

test("concurrent tasks cannot replace each other's state", () => {
  const a = task("a", "RUNNING");
  assert.equal(mergeTask(a, task("b", "FAILED")), a);
});
test("late responses never undo a terminal result", () => {
  for (const status of ["SUCCEEDED", "FAILED"]) {
    const terminal = task("a", status);
    assert.equal(mergeTask(terminal, task("a", "RUNNING", 3)), terminal);
  }
});
test("older responses and queued responses cannot regress running progress", () => {
  const running = task("a", "RUNNING");
  assert.equal(mergeTask(running, task("a", "RUNNING", 1)), running);
  assert.equal(mergeTask(running, task("a", "QUEUED", 3)), running);
  assert.equal(mergeTask(running, task("a", "SUCCEEDED", 3)).status, "SUCCEEDED");
});
test("only unfinished jobs need polling, and query failures back off with a cap", () => {
  assert.equal(active(task("a", "QUEUED")), true);
  assert.equal(active(task("a", "RUNNING")), true);
  assert.equal(active(task("a", "FAILED")), false);
  assert.equal(active(task("a", "SUCCEEDED")), false);
  assert.deepEqual([0, 1, 2, 3, 40].map(pollDelay), [1500, 3000, 6000, 12000, 12000]);
});
test("client upload validation rejects unsupported, empty and oversized files", () => {
  const accepted = ["image/png", "image/jpeg"];
  assert.equal(fileError({ type: "image/png", size: 100 }, accepted, 100), null);
  assert.ok(fileError({ type: "image/png", size: 101 }, accepted, 100));
  assert.ok(fileError({ type: "image/png", size: 0 }, accepted, 100));
  assert.ok(fileError({ type: "image/svg+xml", size: 10 }, accepted, 100));
});
