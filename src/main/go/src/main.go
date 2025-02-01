// main.go
package main

import (
	"fmt"
	"runtime"
)

func main() {
	fmt.Println("Go Environment Test")
	fmt.Printf("Go Version: %s\n", runtime.Version())
	fmt.Printf("OS/Arch: %s/%s\n", runtime.GOOS, runtime.GOARCH)

	// Test slice operations
	numbers := []int{1, 2, 3, 4, 5}
	fmt.Printf("Slice test: %v\n", numbers)
}
