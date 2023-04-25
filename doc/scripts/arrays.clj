(ns structs.arrays)

;; We're going to start with arrays, which are a fundamental data structure in most languages, including all the major
;; host platforms for Clojure. Arrays are usually built out of a block of contiguous memory in a computer,
;; and are viewed as a sequence of boxes for holding data. For instance, a 4 element array would appear as:

;; Depending on the host system, these boxes can contained typed data, such as such as numbers or references,
;; to specific data types. Most systems also allow for general object references, which is common in Clojure.
;; An array of references containing 4 elements would appear as:

;; Let's start by creating an array of 4 elements.

(object-array 4)

;; ## Raw REPLs

;; If you are using a raw Clojure REPL, then the array object returned prints out an internal representation like:
;;
;; `#object["[Ljava.lang.Object;" 0x214894fc "[Ljava.lang.Object;@214894fc"]`

;; REPLs that are integrated into an editor, such as Emacs/CIDER will often update this to display an array like a
;; vector, but with comma separators:
;; `[nil, nil, nil, nil]`

;; If you are ever at a raw REPL, then an easy way to see the contents is convert it into a seq
(seq (object-array 4))

;; However, for consistency we can update a raw REPL to make it consistent with CIDER, by updating the
;; clojure.core/print-method multimethod. Skip this step if you are using an editor integrated with your REPL:
(defmethod print-method (class (object-array 0)) [o ^java.io.Writer w]
  (#'clojure.core/print-sequential "[" #'clojure.core/pr-on ", " "]" o w))

;; ## Working with Arrays
;; Let's save the array into a var, so we can work with it.

(def a (object-array 4))

;; Arrays contain mutable contents, so let us add and view some things:
(aset a 0 "Hello")
(aset a 2 "world")

a

(aset a 1 " ")
(aset a 3 "!")
a

;; Note that we have mutated the contents of the array. This is something we usually want to avoid in Clojure
;; but when working at a low level we will often want to cause mutations in a controlled way until basic operations
;; are complete, and then never change the data again. So for now, we will work with mutation as we move our
;; way to efficient immutability.

;; ## Operations
;; Some other basic operations on arrays include retrieving any element by index:
(aget a 2)
;; And counting the size:
(alength a)

;; Many Clojure functions that expect a seq will convert an array implicitly, so we can also use some seq operations:
(apply str a)
(count a)

;; There is also some support for arrays in other functions that work with Clojure's native collections. This is
;; important, as `aget` uses reflection and is very slow, while other functions access the data directly:
(nth a 0)
(get a 3)

;; Is there much difference?

;; Create an array containing 1 million numbers:
(def test-data (object-array (range 1 1000001)))

;; How long does it take to add up all of these numbers when we access them with different functions?
(time (reduce #(+ %1 (get test-data %2)) 0 (range 1000000)))
(time (reduce #(+ %1 (nth test-data %2)) 0 (range 1000000)))
(time (reduce #(+ %1 (aget test-data %2)) 0 (range 1000000)))


;; `nth` is slightly faster than `get`, but `aget` is significantly slower.

;; Arrays can be used as useful and efficient building blocks for data. For instance, we can create a "pair" of
;; elements:
(defn pair
  [a b]
  (let [p (object-array 2)]
    (aset p 0 a)
    (aset p 1 b)
    p))

(pair "left" "right")

;; Conveniently, Clojure provides the functions `first` and `second` for working with seqs, and these can be used
;; with our new pairs:
(first a)
(second a)

;; ## Structures with Arrays
;; We can use these things similarly to how we would a Cons cell:
(def list-tail (pair "world" nil)) 
(def full-list (pair "hello" list-tail)) 

;; This isn't recognized by Clojure functions as a list, so we can't use it as a seq without adding extra
;; functionality but the structure is the same. But we don't want pairs merely so we can duplicate lists.

;; Instead, we can use simple structures like this in the next section when we build binary trees.
;; trees.clj
