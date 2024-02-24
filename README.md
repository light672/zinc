# zinc 🪨
a type-safe, embedded programming language.

currently available for:
- Kotlin (JVM)
- Java


*Originally made for [Create Robotics](https://github.com/createrobotics/CreateRobotics)*

## Basic Syntax
To declare a variable, use `val` for immutable variables, or `var` for mutable variables.
```kt
val a = 19
var b = 14
a++ // error
b++ // valid
```
Functions can be declared using the `func` keyword.
```kt
func add(a: num, b: num): num {
    a + b
}
```
*The last expression in a function is the return value. Alternatively you can use the `return` keyword.*

Structs can be declared using the `struct` keyword,
```kt
struct MyDataType {
    a: num,
    b: num
}
```
and methods for those structs can be defined using the `impl` keyword.
```kt
impl MyDataType {
    func add(self): num {
        self.a + self.b
    }
}
```
Methods can be called using dot notation,
```kt
val myDataType = MyDataType { a: 15, b: 22 }
println("Method return value is ${myDataType.add()}.")
```
or you can use the method as a function, using the struct name almost like a module.
```kt
val myDataType = MyDataType { a: 15, b: 22 }
println("Method return value is ${MyDataType::add(myDataType)}.)
```
## Scope
Top level code is *not allowed* in Zinc.
```kt
println("Top level code!") // error
```
Instead, use a main function.
```kt
func main() {
    println("In main function!") // valid
}
```
Variables and functions that are declared in top level code are *global*, and can be used anywhere in the program, even before they are defined.
```kt
func main() {
    sayHi()
}

func sayHi() { println("Hi!") }
```
These global declarations can also be used in other files.

Local scopes are created in blocks
```kt
// global scope
func main() {
    // local scope
    {
        // deeper local scope
    }
}
```


more coming...
