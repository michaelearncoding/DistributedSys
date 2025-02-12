// ~/6.5840/
// ├── src/
// │   ├── main/
// │   │   ├── mrcoordinator.go
// │   │   ├── mrworker.go
// │   │   ├── mrsequential.go
// │   │   └── test-mr.sh
// │   ├── mr/
// │   │   ├── coordinator.go    # Put coordinator code here
// │   │   ├── worker.go        # Put worker code here
// │   │   └── rpc.go          # Put RPC definitions here
// │   └── mrapps/
// │       ├── wc.go
// │       ├── indexer.go
// │       └── crash.go
package mr

import (
	"sync"
	"time"
)

type TaskState struct {
	status    string // "idle", "in-progress", "completed"
	startTime time.Time
	workerId  int
}

type Coordinator struct {
	mu          sync.Mutex
	mapFiles    []string
	nReduce     int
	mapTasks    map[int]*TaskState
	reduceTasks map[int]*TaskState
	mapsDone    bool
	done        bool
}

func (c *Coordinator) GetTask(req *TaskRequest, resp *TaskResponse) error {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.checkTimeouts()

	// Assign map tasks first
	if !c.mapsDone {
		for id, state := range c.mapTasks {
			if state.status == "idle" {
				resp.Task = Task{
					Type:    MapTask,
					Id:      id,
					File:    c.mapFiles[id],
					NReduce: c.nReduce,
				}
				c.mapTasks[id] = &TaskState{
					status:    "in-progress",
					startTime: time.Now(),
					workerId:  req.WorkerId,
				}
				return nil
			}
		}

		// Check if all maps are done
		allMapsDone := true
		for _, state := range c.mapTasks {
			if state.status != "completed" {
				allMapsDone = false
				break
			}
		}
		if allMapsDone {
			c.mapsDone = true
		}
	}

	// Then assign reduce tasks
	if c.mapsDone {
		for id, state := range c.reduceTasks {
			if state.status == "idle" {
				resp.Task = Task{
					Type: ReduceTask,
					Id:   id,
					NMap: len(c.mapFiles),
				}
				c.reduceTasks[id] = &TaskState{
					status:    "in-progress",
					startTime: time.Now(),
					workerId:  req.WorkerId,
				}
				return nil
			}
		}

		// Check if all reduces are done
		allReducesDone := true
		for _, state := range c.reduceTasks {
			if state.status != "completed" {
				allReducesDone = false
				break
			}
		}
		if allReducesDone {
			c.done = true
			resp.Task = Task{Type: ExitTask}
			return nil
		}
	}

	resp.Task = Task{Type: WaitTask}
	return nil
}

func (c *Coordinator) TaskCompleted(args *TaskRequest, reply *TaskResponse) error {
	c.mu.Lock()
	defer c.mu.Unlock()

	if !c.mapsDone {
		if state, exists := c.mapTasks[args.TaskId]; exists &&
			state.workerId == args.WorkerId {
			c.mapTasks[args.TaskId].status = "completed"
		}
	} else {
		if state, exists := c.reduceTasks[args.TaskId]; exists &&
			state.workerId == args.WorkerId {
			c.reduceTasks[args.TaskId].status = "completed"
		}
	}
	return nil
}

func (c *Coordinator) checkTimeouts() {
	timeout := 10 * time.Second
	now := time.Now()

	if !c.mapsDone {
		for id, state := range c.mapTasks {
			if state.status == "in-progress" &&
				now.Sub(state.startTime) > timeout {
				c.mapTasks[id].status = "idle"
			}
		}
	} else {
		for id, state := range c.reduceTasks {
			if state.status == "in-progress" &&
				now.Sub(state.startTime) > timeout {
				c.reduceTasks[id].status = "idle"
			}
		}
	}
}
