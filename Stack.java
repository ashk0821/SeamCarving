import java.util.ArrayList;
import java.util.Arrays;

import tester.Tester;

// Represents a stack data structure where elements are last in, first out
class Stack<T> {
  ArrayList<T> contents;
  
  Stack(){
    this.contents = new ArrayList<T>();
  }
  
  Stack(ArrayList<T> contents){
    this.contents = contents;
  }
  
  //adds an item to the head of the list
  void push(T item) {
    contents.add(0, item);
  } 
  
  boolean isEmpty() {
    return (contents.size() == 0);
  }
  
  // removes and returns the head of the list
  T pop() {
    return contents.remove(0);
  }
}

class ExamplesStack {
  Stack<Integer> stackInt = new Stack<Integer>();
  Stack<String> stackString = new Stack<String>(new ArrayList<String>(Arrays.asList("hi", "bye")));
  
  void testStack(Tester t) {
    stackInt.push(1);
    stackInt.push(2);
    stackInt.push(3);
    stackInt.push(4);
    t.checkExpect(stackInt.isEmpty(), false);
    t.checkExpect(stackInt.pop(), 4);
    t.checkExpect(stackInt.pop(), 3);
    t.checkExpect(stackInt.pop(), 2);
    t.checkExpect(stackInt.pop(), 1);
    t.checkExpect(stackInt.isEmpty(), true);
    
    stackString.push("hey");
    t.checkExpect(stackString.pop(), "hey");
    t.checkExpect(stackString.pop(), "hi");
    t.checkExpect(stackString.pop(), "bye");
    
  }
}

