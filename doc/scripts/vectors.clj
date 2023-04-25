;; This is a review of the talk presented at :clojureD 2021 "Immutable Data Structures for Fun and Profit".

;; https://youtu.be/oD1WONpv6Xc

;; # Introduction

;; We rarely use mutable arrays in Clojure. Instead, we use immutable vectors for everything from the syntax of
;; the language through to basic data structures. How do these work?

;; At face value, they look a little like an array. They contain a sequence of numbers, and can easily be
;; converted to a seq. Like arrays, they have efficient random access (using `get` or `nth`), and save their
;; length for fact access with `count`.

;; Values in the middle of a vector can be updated using `assoc` rather than `aset`. For instance, to update
;; offset 2 to 101:
(assoc [0 1 2 3 4] 2 101)

;; The major differences are:
;; * Can append:
(conj [0 1 2] 3)
;; * Can truncate:
(pop [0 1 2 3])

;; * Immutable
(def v [0 1 2 3 4])
(assoc v 2 101)
v

;; All of this is accomplished with data structures built on trees of small arrays. The arrays are used for
;; speed and efficiency and they are kept small to enable efficient immutability.

;; # Simple Vector
;; When vectors are small, they can be stored entirely within a small array. This array has a maximum of 32
;; entries. Whenever a modification occurs on these vectors, an entirely new array of up to 32 elements is
;; allocated, and data is just copied into them. This is really not expensive, due to the small size of the array.

;; We can see this by using Java interop to look inside the vector:
(.tail [4 6 8])

(def v [4 6 8 10 12])
(.tail (conj v 14))

;; Of course, the new vector contains a copied array, and original array was not touched
(.tail v)

;; This is all done with a set of basic operations where a new vector is created with a new array, and the
;; necessary data is copied into it. Vectors do not need to use elaborate tricks for small arrays.

;; A vector will start to use more complex structures once it gets larger than 32 elements.
;; At 32 elements, everything looks the same:
(.tail (vec (range 32)))

;; But at 33 elements, we see something new:
(.tail (vec (range 33)))

;; Vectors store all their data in a tree structure that containrs of arrays of exactly 32 elements.
;; Whatever remainder there was from the tree is stored in this final `tail` array. There is always a tail
;; array, containing from 1 to 32 elements. If a vector with a full tail tries to add another element,
;; then the existing full tail becomes part of the tree, and a new tail array is created.

;; To see this, we can see what happens when adding an element to a vector with a full tail. Start by
;; storing that array...
(def full-vec (vec (range 32)))

;; We have already seen that the `full-vec` has a completely full `tail` vector, but there is also
;; a tree that can also be inspected. In this case, the tree should not have any data.
;; The tree starts at the `.root` for the vector, and we will look in the `array` field:
(.array (.root full-vec))

;; The array is there, but unused.

;; By adding one more element to the vector, we have already seen how the tail will only contain this final
;; element. The original 32 elements get moved into the tree. We can see this in the array of the root
;; of the tree:
(def next-vec (conj full-vec 32))
(.array (.root next-vec))

;; The first element of this array contains a child node, and all the rest are `nil`. This child, in turn
;; has its own array. This is getting deeply nested, so we can move to a threading syntax:
(-> next-vec .root .array first .array)

;; This looks like the tail of the original vector. In fact, this full array from `full-vec` was inserted
;; directly into the tree:
(identical? (-> next-vec .root .array first .array) (.tail full-vec))

;; The structure of the new vector looks like this:
;; file:../../images/vectors.html#32

;; As more and more data is added to the vector, each version will reuse these nodes, and only allocate for
;; themselves a new array to hold the expanding tail. This continues all the way up to when the tail is
;; 32 elements long again, for a total vector length of 64:

(def vec64 (into next-vec (range 33 64)))

(-> vec64 .root .array first .array)
(.tail vec64)

;; And that initial array containing 0-31 is an identical object for both vec64 and next-vec
(identical? (-> vec64 .root .array first .array) (-> next-vec .root .array first .array))

;; Adding one more to this longer vec64 is just like the previous occasion.
;; The tail will be packaged into a Node, and that node will be added to the tree in the second
;; location of the root node.

(def vec65 (conj vec64 64))
(-> vec65 .root .array second .array)

;; The array in the Node is the same object as the tail from the previous `vec64`:
(identical? (-> vec65 .root .array second .array) (.tail vec64))

;; Finally, the new tail for `vec65` will contain the single value that was added:
(.tail vec65)

;; The structure for `vec65` now looks like this:
;; file:../../images/vectors.html#65

;; This process can continue until the root node is full. A vector of this size will have a root that points to
;; 32 separate child nodes, each with 32 entries, for a total of 1024 entries in the tree. Then, when the tail
;; has 32 entries the full size of a vector of this structure will be 1024+32=1056. To demonstrate this
;; we can expand the original vector to contain the numbers 0-1055.

(def vec1056 (into vec65 (range 65 1056)))

;; Now we have a full root of the tree:
(.root vec1056)
;; and the tail is full:
(.tail vec1056)

;; Adding to a vector of this size will package up the tail into a node again, but this time it can't fit on the
;; end of the array in the root node. Instead, a new root is created, and the old root will be its first child.
;; The tree will now have 3 levels of nodes. The entire previous tree will now be reference by the first array
;; element of the new root.

(def vec1057 (conj vec1056 1056))
(-> vec1057 .root .array)
(identical? (-> vec1056 .root) (-> vec1057 .root .array first))

;; In fact, the array at the beginning of this is still the original tail array from the original vector:

(identical? (-> vec1057 .root .array first .array first .array)
            (.tail full-vec))

;; The deeper tree structure is shown here, with dots to indicate extra arrows and nodes in the tree, since they
;; can't all fit in the image.
;; file:../../images/vectors.html#1057

;; Following this structure, we can see the contents of the bottom right node, by starting with the second
;; array element in the root node:
(-> vec1057 .root .array second .array first .array)

;; #### Exercise:
;; Write a function called vth that can manually retrieve the nth element from a vector that contains
;; between 1057 and 32800 elements. i.e. There are 3 levels.

;; _Solution goes here_

(vth vec1057 500)
;; 500
(vth vec1057 1055)
;; 1055
;; Now check the tail:
(vth vec1057 1056)
;; 1056

;; Vectors can grow indefinitely, by making the tree deeper each time all available space in the tree is occupied.
;; Each level multiplies the available size of the tree by 32. The tree can hold over 33 million elements with
;; only 5 levels. At only 13 levels deep, the tree would have the capacity to hold more than addressable memory in
;; a 64 bit system, so the overhead in navigating the tree is never very high.

;; Importantly, updating the tree will only ever modify a single leaf node, along with creating copies of the parent
;; nodes all the way to the root. For the 1057-element vector shown here, that only requires copying/modifying 3
;; nodes. Which means very little complexity overhead in time or space when modifying a large vector.

;; ## Next
;; Understanding this structure is very useful for understanding how Clojure implements one of its most important structures: 
;; the HashMap
