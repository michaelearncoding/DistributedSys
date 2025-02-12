// src/mr/worker.go
package mr

import (
	"encoding/json"
	"fmt"
	"hash/fnv"
	"io/ioutil"
	"net/rpc"
	"os"
	"sort"
	"time"
)

type KeyValue struct {
	Key   string
	Value string
}

func getTask(workerId int) Task {
	args := TaskRequest{WorkerId: workerId}
	reply := TaskResponse{}
	ok := call("Coordinator.GetTask", &args, &reply)
	if !ok {
		return Task{Type: ExitTask}
	}
	return reply.Task
}

// RPC call helper function
func call(rpcname string, args interface{}, reply interface{}) bool {
	sockname := coordinatorSock()
	c, err := rpc.DialHTTP("unix", sockname)
	if err != nil {
		return false
	}
	defer c.Close()

	err = c.Call(rpcname, args, reply)
	if err == nil {
		return true
	}
	return false
}

func doReduce(task Task, reducef func(string, []string) string) {
	intermediate := []KeyValue{}

	// Read intermediate files
	for i := 0; i < task.NMap; i++ {
		filename := fmt.Sprintf("mr-%d-%d", i, task.Id)
		file, _ := os.Open(filename)
		dec := json.NewDecoder(file)
		for {
			var kv KeyValue
			if err := dec.Decode(&kv); err != nil {
				break
			}
			intermediate = append(intermediate, kv)
		}
		file.Close()
	}

	// Sort by key
	sort.Slice(intermediate, func(i, j int) bool {
		return intermediate[i].Key < intermediate[j].Key
	})

	// Create output file
	oname := fmt.Sprintf("mr-out-%d", task.Id)
	ofile, _ := ioutil.TempFile("", "mr-tmp-*")

	// Process each key group
	i := 0
	for i < len(intermediate) {
		j := i + 1
		for j < len(intermediate) && intermediate[j].Key == intermediate[i].Key {
			j++
		}
		values := []string{}
		for k := i; k < j; k++ {
			values = append(values, intermediate[k].Value)
		}
		output := reducef(intermediate[i].Key, values)
		fmt.Fprintf(ofile, "%v %v\n", intermediate[i].Key, output)
		i = j
	}

	os.Rename(ofile.Name(), oname)
}

// Unix domain socket name
func coordinatorSock() string {
	s := "/var/tmp/5840-mr-"
	s += os.Getenv("USER")
	return s
}

// src/mr/worker.go
// Add after existing code:

func Worker(mapf func(string, string) []KeyValue,
	reducef func(string, []string) string) {

	workerId := os.Getpid()

	for {
		task := getTask(workerId)

		switch task.Type {
		case MapTask:
			doMap(task, mapf)
			reportTaskComplete(task.Id, workerId)
		case ReduceTask:
			doReduce(task, reducef)
			reportTaskComplete(task.Id, workerId)
		case WaitTask:
			time.Sleep(time.Second)
		case ExitTask:
			return
		}
	}
}

func doMap(task Task, mapf func(string, string) []KeyValue) {
	// Read input file
	content, err := ioutil.ReadFile(task.File)
	if err != nil {
		return
	}

	// Call user's map function
	kva := mapf(task.File, string(content))

	// Create intermediate files
	intermediate := make([][]KeyValue, task.NReduce)
	for _, kv := range kva {
		bucket := ihash(kv.Key) % task.NReduce
		intermediate[bucket] = append(intermediate[bucket], kv)
	}

	// Write to files
	for i := 0; i < task.NReduce; i++ {
		oname := fmt.Sprintf("mr-%d-%d", task.Id, i)
		ofile, _ := ioutil.TempFile("", "mr-tmp-*")
		enc := json.NewEncoder(ofile)
		for _, kv := range intermediate[i] {
			enc.Encode(&kv)
		}
		os.Rename(ofile.Name(), oname)
	}
}

func reportTaskComplete(taskId int, workerId int) {
	args := TaskRequest{
		WorkerId: workerId,
		TaskId:   taskId,
	}
	reply := TaskResponse{}
	call("Coordinator.TaskCompleted", &args, &reply)
}

func ihash(key string) int {
	h := fnv.New32a()
	h.Write([]byte(key))
	return int(h.Sum32() & 0x7fffffff)
}
