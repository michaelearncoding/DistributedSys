// rpc.go
package mr

type TaskType int

const (
	MapTask TaskType = iota
	ReduceTask
	WaitTask
	ExitTask
)

type Task struct {
	Type    TaskType
	Id      int
	File    string
	NReduce int
	NMap    int
}

type TaskRequest struct {
	WorkerId int
}

type TaskResponse struct {
	Task Task
}

// Task request from worker to coordinator
type TaskRequest struct {
	WorkerId int
	TaskId   int // Add TaskId field
}

// Task response from coordinator to worker
type TaskResponse struct {
	Task Task
}
