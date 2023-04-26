
;; Let's start saving some words to an array: "honeydew", "carrot", "grapes", "edamame"
;; Start with a 10 element array:
(def words (object-array 10))

;; We want to put the strings into a predetermined position, so let's do it according to the first letter
(first "grapes")

;; If we convert this to ascii we can get a numerical value:
(int (first "grapes"))

;; The first letter is `a`, so let's treat the letter as an offset:
(- (int (first "grapes")) (int \a))

;; With letters all having an offset from `a`, we can use this to determine a position to put each string into
;; the array. So the string "grapes" can be at position 6 in the array. This can be captured in a function:
(defn array-location
  [s]
  (- (int (first s)) (int \a)))

;; Now we build an operation that can modify the array to insert a string at this offset:
(defn insert!
  [arr s]
  (aset arr (array-location s) s))

;; So long as we insert words that start with letters up to `j` then we will stay within the bounds of the array.

(insert! words "honeydew")
words

(insert! words "carrot")
(insert! words "grapes")
(insert! words "edamame")

words

;; Now that we have a way to insert a string into the array, how can we see if a word is present?
;; When presented with a word, we already know where to look, because of the first letter.
;; We can check if the string found there matches, and if it does, then we know it was present.

(defn contains-object?
  [arr s]
  (let [found (nth arr (array-location s))]
    (= found s)))

;; Let's test this:
(contains-object? words "carrot")
(contains-object? words "apple")
(contains-object? words "grapes")
(contains-object? words "figs")

;; The importance of this approach is that we find the insertion location quickly, and can check for existence
;; just as quickly. For structures with lots of entries, this is much faster than searching through the structure
;; to find where something should be inserted or if it is already there.

;; ## Data Domain
;; Unfortunately, we're still restricted to strings that start with letters before `k`.
;; We can't even deal with capital letters:
(array-location "Banana")

(try
  (contains-object? words "Banana")
  (catch Exception e e))

;; The simplest approach is to just wrap the position around to the beginning of the array. That way something
;; starting with `k` does not go to position `10`, but wraps back to 0. Similarly `l`, `m`, and `n` should wrap
;; back to `1`, `2`, and `3`.

;; But we can't just subtract 10, because once we get to `u`, which is the 21st letter, we will need to subtract 20.
;; Instead, we need to divide by 10, and take the remainder. This is the `mod` operator. Let's update the
;; `array-location` function to accept the size of the array so we can do this:

(defn array-location
  [size s]
  (mod (- (int (first s)) (int \a)) size))

;; we need to tell `insert!` and `contains-object?` to pass this value along:

(defn insert!
  [arr s]
  (aset arr (array-location (count arr) s) s))

(defn contains-object?
  [arr s]
  (let [found (nth arr (array-location (count arr) s))]
    (= found s)))

;; Now check if we can insert other strings:

(insert! words "kiwi")
(insert! words "Banana")

words

;; Since any offset is now guaranteed to within bounds of the array, we don't need to care about starting
;; with `a` at 0, so we can get rid of that part of the position calculation:
(defn array-location
  [size s]
  (mod (int (first s)) size))

;; Now we can start again, with the new order:
(def words (object-array 10))
(doseq [word ["honeydew" "carrot" "grapes" "edamame" "kiwi" "Banana"]]
  (insert! words word))
words

(contains-object? words "grapes")
(contains-object? words "Banana")
(contains-object? words "kiwi")
(contains-object? words "kiwifruit")

;; ## Duplicates
;; If another string hits the same position in the array, then this is called a _collision_.
;; For instance, "apple" collides with "kiwi".

(insert! words "apple")
words

;; We need a strategy for dealing with such collisions. There are a few, but if we look at what the Java libraries
;; did in the earlier releases, we can save a linked list of items in the position, instead of just single values.
;; This can be done by `cons`ing the element to whatever is found at that point. We also want to check if the data
;; is already there.

;; #### Exercise
;; Rewrite `insert!` and `contains-object?` to use a linked list at each array location

;; _Solution goes here_

;; Let's test this by repopulate the array of words, and adding "apple":
(def words (object-array 10))
(doseq [word ["honeydew" "carrot" "grapes" "edamame" "kiwi" "Banana" "apple"]]
  (insert! words word))
words

;; Each entry is now a list. Most are just 1 element, but we can see the `("apple" "kiwi")` list with 2 elements
;; towards the end of the array.

;; Now we can check if the data can still be found:
(contains-object? words "apple")
(contains-object? words "edamame")
(contains-object? words "kiwi")
(contains-object? words "kiwifruit")

;; ## Sequencing
;; If we need to see all of the elements in this set, then how can we go about it? This is just an array of lists
;; so we can just concat all the lists. Fortunately, `nil` is an empty list so those will be dropped. As with
;; many other functions, the array can be converted to a seq automatically, so we can just
;; `apply` the `concat` function:
(apply concat words)

;; ## Overload!
;; Handling collisions with a linked list that can be searched linearly does provide a quick fix, but if the list
;; gets too long, then we lose all the benefits of finding the data quickly:
(doseq [word ["ugli" "Cherry" "Mandarin" "Watermelon" "apricot" "kumquat" "ugni"]]
  (insert! words word))
words

;; If we look for "carrot" we can go directly to the 9th array element, and find it immediately. But if we search for
;; "kiwi" we go to the 7th element of the array, then search linearly all the way down 9 elements of the list to
;; find it. If it wasn't there, then the entire list would be traversed.
;; Also, any new additions are likely to hit existing data in this small array,  making the lists longer each time.
;; Ideally, we want a mostly empty array when inserting new data, and short lists will make searching faster.
;; The measurement of collisions in a set is known as the _load factor_, and we want to keep the load low.
;; Java uses a default load factor of 0.75

;; Try making the array 5 times bigger, and insert everything into that:
(def words50 (object-array 50))
(doseq [word (apply concat words)]
  (insert! words50 word))
words50

;; This is process is called _rehashing_. It is a slow process, but when it gets averaged across all accesses,
;; it has an _amalgamated complexity_ of linear insertion time.
;; The result is that elements are spread out a lot more, but there are still several lists with 2 elements,
;; and one of them has 3. We want a better way to spread out the data.

;; Up to now, we have used just the first character of the string to determine the location. This won't help strings
;; like "apple" and "apricot", despite the rest of the string being very different. To recognize this, the
;; entire string should be considered. Fortunately, we don't care about the magnitude of the raw number that is
;; generated, since the position will always be the generated number mod of the size of the array.
;; For instance, we can just add the numbers of each character in the string:
(apply + (map int "apple"))
(apply + (map int "apricot"))

;; An alternative to `apply` is `reduce`:
(reduce #(+ %1 (int %2)) 0 "apple")

;; Let's use this new operation to determine the location in the array:
(defn array-location
  [size s]
  (mod (apply + (map int s)) size))

;; Let's insert with this new position scheme:
(def words50 (object-array 50))
(doseq [word (apply concat words)]
  (insert! words50 word))
words50

;; This is spread out better: There is only a single collision!
;; However, we are still going to collide a lot, particularly for anagrams:
(insert! words50 "lemon")
(insert! words50 "melon")
words50

;; If you search down the list to position 39 you'll see the list `("melon" "lemon")`.

;; Instead of trying to spread the numbers for each word out ourselves, Clojure and Java come with a much more general
;; calculation called the `hash`. For strings, this works by converting the string to bytes, and multiplying
;; the accumulated number by 31 at each step.
;; For a simple ascii string, this comes to:
(defn my-hash
  [s]
  (reduce #(+ (* 31 %1) (int %2)) 0 s))

(my-hash "apple")
;; Compare this to Java's hash code:
(.hashCode "apple")

;; Clojure actually implements its own variation, based on the Murmur3 library from Guava. It starts with
;; the standard hash, and then runs it through a multiply and rotation. I won't describe it all, but here
;; is the operation in code:
(defn my-clojure-hash
  [s]
  (let [input (my-hash s)
        input (bit-and 0xffffffff (* 0xcc9e2d51 input))
        input (Integer/rotateLeft (int input) 15)
        k1 (bit-and 0xffffffff (* 0x1b873593 input))
        h1 (bit-xor 0 (int k1))
        h1 (Integer/rotateLeft (int h1) 13)
        h1 (bit-and 0xffffffff (+ 0xe6546b64 (* 5 h1)))
        h1 (bit-xor h1 4)
        h1 (bit-xor h1 (unsigned-bit-shift-right h1 16))
        h1 (bit-and 0xffffffff (* h1 0x85ebca6b))
        h1 (bit-xor h1 (unsigned-bit-shift-right h1 13))
        h1 (bit-and 0xffffffff (* h1 0xc2b2ae35))]
    (bit-xor h1 (unsigned-bit-shift-right h1 16))))

(my-clojure-hash "apple")
(hash "apple")

;; Maybe that's more than is needed for this application, but it's provided for free by Clojure, so let's use it.

;; #### Exercise
;; Rewrite `array-location` to use the `hash` function

;; _Solution goes here_

;; When using a REPL, the `insert` and `contains-object?` functions will pick up this new version of `array-location`
;; (defn insert!
;;   [arr s]
;;   (let [pos (array-location (count arr) s)
;;         existing (nth arr pos)]
;;     (if (some #(= s %) existing)
;;       arr
;;       (aset arr pos (cons s existing)))))

;; (defn contains-object?
;;   [arr s]
;;   (let [found (nth arr (array-location (count arr) s))]
;;     (some #(= s %) found)))


(def words50 (object-array 50))
(doseq [word (apply concat ["lemon" "melon"] words)]
  (insert! words50 word))
words50

;; This is not all that much different at this scale, but none of the lists are longer than 2, so this still looks
;; reasonable.

;; Using hashes to store data in an array for quick storage and retrieval like this is called a Hash Set.

;; ## Datatypes
;; Now that we are based on universal hash codes and not characters in a string, there is nothing stopping us from
;; inserting other types into the array. So, for instance, we can add numbers and keywords:
(insert! words50 42)
words50

;; This went into offset 6 of the array.

;; ## Maps
;; Just like with trees, a map is a set that stores a key/value pair, however insertion and searching is performed
;; by only using the key.

;; #### Exercise:
;; Create `insert!` and `get-map` to use an array as a Hash Map. Note that the key may already existi
;; with a different value. Use the following helper function:

(defn pair
  [k v]
  (let [p (object-array 2)]
    (aset p 0 k)
    (aset p 1 v)
    p))

;; _Solution goes here_

;; Let's test this out with an array of size 50:
(def map-array (object-array 50))
;; ... and map some number names to their numeric equivalents
(insert-map! map-array "zero" 0)
(insert-map! map-array "one" 1)
(insert-map! map-array "two" 2)
(insert-map! map-array "three" 3)
(insert-map! map-array "four" 4)
(insert-map! map-array "five" 5)
(insert-map! map-array "six" 6)
(insert-map! map-array "seven" 7)
(insert-map! map-array "eight" 8)
(insert-map! map-array "nine" 9)

;; Now we should have an array that has these pairs scattered throughout:
map-array

;; And we can get back what we stored:
(get-map map-array "five")
(get-map map-array "seven")
(get-map map-array "ten")

;; #### Exercise:
;; Write a `to-seq` function that will convert the hashmap array to a seq of pairs.
;; Convert these pairs to 2 element vectors.

;; _Solution goes here_

;; Apply this to the map-array:
(to-seq map-array)

;; #### Exercise:
;; Write a `map-keys` function and a `map-vals` function to duplicate the
;; `keys` and `vals` functions for maps:

;; _Solution goes here_

;; ## Immutability
;; In order to follow how to make these structures immutable, let's turn to Tree structures.

;; As a first iteration, let's use Vectors rather than arrays. We can update values in a Vector
;; using `assoc`. We also get a whole lot of Clojure idioms because vectors are built into the
;; language. So let's start by rewriting each of the map functions. As well as the `insert` function
;; that takes both a key and value just like `assoc`, We can also write another arity 
;; that takes a key/value as a pair, which will make it easier to insert data.

;; #### Exercise
;; Write the functions `insert` and `get-map` to duplicate the previous functionality, but
;; now the hashmap is in a vector, and the operations must be immutable.

;; _Solution goes here_

;; Initialize a vector at the full width, full of nils:
(def vctr (vec (repeat 50 nil)))

;; We can use `reduce` to add all of the elements of out map-array
(def vmap (reduce insert vctr (to-seq map-array)))
vmap
;; The only difference is that vectors are not printed with commas

(get-map vmap "three")
(get-map vmap "five")
(get-map vmap "eleven")

;; ## Space
;; This works exactly as expected, and is fully immutable. But what about all that space filled with nils?
;; This is trivial overhead for small vectors of 50, but what about when the hashmap is expected to hold
;; thousands or millions of entries? In order to avoid collisions, this requires a vector at least an
;; order of magnitude greater in size, most of which will be empty space.

;; To avoid this, Clojure's HashMaps use a vector-like approach of a tree with an array in each node.
;; Hashing then occurs at each level down the tree of arrays.
;;
;; Let's start with a small hashmap of 9 items:
(def hm (reduce #(assoc %1 (keyword (str "n" %2)) %2) {} (range 1 10)))
hm
;; {:n9 9, :n6 6, :n3 3, :n4 4, :n2 2, :n8 8, :n5 5, :n7 7, :n1 1}

;; We can also see the type:
(type hm)
;; clojure.lang.PersistentHashMap

;; Clojure hides the structure of hashmaps from the user, so for educational purposes
;; I have a special version of `clojure.lang.PersistentHashMap` with public fields.
;; We can start by looking at the root of this object:
(type (.root hm))
;; clojure.lang.PersistentHashMap$BitmapIndexedNode

;; This object has an array associated with it:
(-> hm .root .array)
;; [:n9, 9, :n6, 6, :n3, 3, :n4, 4, :n2, 2, :n8, 8, :n5, 5, :n7, 7, :n1, 1]

;; First of all, we can see that it does not use an array of key/value pairs. Instead, it just puts the keys
;; into the even positions, and the values into the odd positions.
;; The second difference here is determining where in the array each element can be found.
;; This uses a slightly different hashing process for identifying the array location, based on a bitmap
;; representation of data stored in the array, stored in the .bitmap field.
(-> hm .root .bitmap)
;; 572658195

;; This can be viewed as a bitmap by printing it in binary, or base 2:
(Integer/toString (-> hm .root .bitmap) 2)
;; "100010001000100001001000010011"

;; The position of an entry in the array starts with the bottom 5 bits (representing numbers from 0 to 31)
;; of the hash, which will define the number of bits that will be read out of the bitmap.
;; We can get this with a `mask` function. Later on we will also want to consider masking other groups
;; of 5 bits, so we will bring in an extra parameter to shift those bits down.
(defn mask
  [key shift]
  (bit-and (unsigned-bit-shift-right (hash key) shift) 0x1f))

;; The resulting number leads to the number of bits to read from the bitmap, and counting the number of 1s
;; will lead to the position:
(defn pos [node k shift]
 (let [bit (bit-shift-left 1 (mask k shift))]
   (Integer/bitCount (bit-and (-> node .bitmap) (dec bit)))))

;; This position calculation is a complex mechanism that I'm not going to fully describe here,
;; but it is described in Phil Bagwell's Hash Trees:
;; https://hashingit.com/elements/research-resources/2001-ideal-hash-trees.pdf
;;
;; This is an extention of Array Mapped Tries which were also designed by Phil Bagwell:
;; https://infoscience.epfl.ch/record/64394/files/triesearches.pdf
;;
;; The main benefit here is that the arrays can be kept short, without lots of empty spaces in them.
;; Let's try this on one of the keys. I will select `:n8`
(pos (.root hm) :n8 0)
;; 5

;; This number has to be doubled to account for everything being stored in pairs, and then used as an offset
;; into the array for the key, followed by the value:
(nth (-> hm .root .array) (* 2 5))
;; :n8
(nth (-> hm .root .array) (inc (* 2 5)))
;; 8

;; But this array is constrained to not get too large. This leads to collisions before long, so what happens
;; when this happens?
;; In this case, another tree node will be placed in that location. We can see this if we start the range at 0:

(def hm2 (reduce #(assoc %1 (keyword (str "n" %2)) %2) {} (range 10)))

;; This contains a single collision, between :n0 and :n4, which were at offsets 6 and 7 in the original array
(-> hm2 .root .array)
;; [:n9, 9, :n6, 6, :n3, 3, nil, #object[clojure.lang.PersistentHashMap$BitmapIndexedNode 0xbc4d5e1 "clojure.lang.PersistentHashMap$BitmapIndexedNode@bc4d5e1"], :n2, 2, :n8, 8, :n5, 5, :n7, 7, :n1, 1]

;; This time the key is `nil` and the value is another node. Looking in that node, we can see the two entries,
;; and a small amount of unused space:
(-> hm2 .root .array (nth 7) .array)
;; [:n4, 4, :n0, 0, nil, nil, nil, nil]

;; The location in the top level led to a collision, so how will an entry be found at this level?
;; Recall that only the bottom 5 bits were used at the first level. This is the second level down, so the next 5 bits
;; can be used. That means using the `shift` parameter that was previously 0. This time, we want to shift
;; by 5 bits. Also, the node being searched must be specified, in order to get its bitmap:
(pos (-> hm2 .root .array (nth 7)) :n4 5)
;; 0
(pos (-> hm2 .root .array (nth 7)) :n0 5)
;; 1

;; But there is more to this structure. Notice how the key at the top level was left as `nil` to indicate that
;; the value was another node to search. A deep tree can be built out of this, but half of every array would be
;; set to `nil`, which is unnecessary. Instead, when there are only tree nodes in an array and no key/value pairs,
;; a different kind of tree node is used: the ArrayNode.
;; Let's create a big hashmap with 1000 entries:
(def hb (reduce #(assoc %1 (keyword (str "n" %2)) %2) {} (range 1000)))
;; And look at the root:
(type (.root hb))
;; clojure.lang.PersistentHashMap$ArrayNode

;; These nodes have no key/value pairs, but instead hold just the structures where those values collide. The
;; arrays for these nodes are always sized at 32:
(-> hb .root .array count)
;; 32

;; The contents of this root array is other tree nodes:
(-> hb .root .array)
;; [#object[clojure.lang.PersistentHashMap$ArrayNode 0x7cf283e1 "clojure.lang.PersistentHashMap$ArrayNode@7cf283e1"], #object[clojure.lang.PersistentHashMap$ArrayNode 0x20e6c4dc "clojure.lang.PersistentHashMap$ArrayNode@20e6c4dc"], ...

;; Importantly, this array can only contain INodes, which is an interface implemented by BitmapIndexedNodes and ArrayNodes:
(type (-> hb .root .array))
;; [Lclojure.lang.PersistentHashMap$INode;

;; Let's look for some values.
;; Because ArrayNodes always have 32 elements, then the fancy trick to minimize the size of the array is not
;; needed, and the bottom 5 bits can be used directly. This just uses the `mask` function`:
(mask :n33 0)
;; 9

;; There are many collisions here, with examples at :n809 and :n200
(mask :n809 0)
;; 9
(mask :n200 0)
;; 9

;; Before looking into the tree, let's create a helper function that with get a node's nth element from its array:
(defn nnth [node n] (-> node .array (nth n)))

;; A partial outline of the tree is at:
;; ../../images/hash.html

;; Looking down this branch at offset 9, we can start to see some empty nodes in the tree:
(-> hb .root (nnth 9) .array)
;; [nil, #object[clojure.lang.PersistentHashMap$BitmapIndexedNode 0x26a2f7f9 "clojure.lang.PersistentHashMap$BitmapIndexedNode@26a2f7f9"], nil, ...

;; This is a little difficult to read, so the empty node offsets can be seen at:
(->> (-> hb .root (nnth 9) .array) (keep-indexed #(when-not %2 %1)))

;; But this is still an ArrayNode, so we need to go down another level. The location of the next level is
;; found using the next 5 bits, so we shift the mask by 5:
(mask :n33 5)
;; 17

(mask :n809 5)
;; 17

(mask :n200 5)
;; 1

;; Looking in the 17 element, we're back to the BitmapIndexedNode with the following array:
(-> hb .root (nnth 9) (nnth 17) .array)
;; [:n33, 33, :n782, 782, :n809, 809]

;; This contains :n33 and :n809, as expected! We should also expect them to have positions
;; of 0 and 2 respectively. We need to provide the node as an argument (to get the bitmap),
;; and because we are down another level, the shift is now 10:
(let [node (-> hb .root (nnth 9) (nnth 17))]
  (pos node :n33 10))
;; 0
(let [node (-> hb .root (nnth 9) (nnth 17))]
  (pos node :n809 10))
;; 2

;; Similarly, we can find the :n200 entry, starting with the node at position 1:
(-> hb .root (nnth 9) (nnth 1) .array)
;; [:n200 200]
;; And then this node is provided when calculating the position:
(pos (-> hb .root (nnth 9) (nnth 1)) :n200 10)
;; 0

;; This can be encoded into a function:

(defn get2
  [mp key]
  (loop [node (.root mp) shift 0]
    ;; if there is no node, then there is no matching entry
    (and node
      (let [array (.array node)]
        ;; check the node type
        (if (instance? clojure.lang.PersistentHashMap$ArrayNode node)
          ;; Find the ArrayNode entry and recurse down the tree
          (let [entry-pos (mask key shift)]
            (recur (nth array entry-pos) (+ shift 5)))
          ;; Find the position in a BitmapIndexNode
          (let [entry-pos (* 2 (pos node key shift))]
            ;; If the position is not in the array, then there is no matching entry
            (when (< entry-pos (count array))
              ;; read the key position
              (let [entry (nth array entry-pos)]
                (if (nil? entry)
                  ;; nil keys mean the data contains a tree to recurse into
                  (recur (nth array (inc entry-pos)) (+ shift 5))
                  ;; does the key match? It should!
                  (when (= entry key)
                    (nth array (inc entry-pos))))))))))))

(get2 hb :n100)
;; 100
(get2 hb :n555)
;; 555
(get2 hb :n1001)
;; nil

;; It also works on the small 1 and 2 level trees:
(get2 hm :n4)
;; 4
(get2 hm :n0)
;; nil

(get2 hm2 :n4)
;; 4
(get2 hm2 :n0)
;; 0

;; Let's try modifying the graph by adding a node :n1336
;; Where will this go in the array?
(mask :n1336 0)
;; 9
(mask :n1336 5)
;; 17

;; This is that same 9-17 path we looked at earlier:
(-> hb .root (nnth 9) (nnth 17) .array)
;; [:n33, 33, :n782, 782, :n809, 809]

;; Now do the insertion:
(def hb2 (assoc hb :n1336 1336))

;; Use this convenience function to compare if the nth array elements
;; from 2 different nodes are the same:
(defn identical-nth? [node1 node2 n] (identical? (nnth node1 n) (nnth node2 n)))

;; Now compare the root arrays of both maps:
(->> (range 32) (map (partial identical-nth? (.root hb) (.root hb2))))
;; (true true true true true true true true true false true true true true true true true true true true true true true true true true true true true true true true)

;; These differ on the node at position 9. Let's capture those nodes from each map
(def hbn (-> hb .root (nnth 9)))
(def hbn2 (-> hb2 .root (nnth 9)))

;; And we can compare their contents the same way:
(->> (range 32) (map (partial identical-nth? hbn hbn2)))
;; (true true true true true true true true true true true true true true true true true false true true true true true true true true true true true true true true)

;; This time, they only differ on node 17, so let's look at those:
(-> hbn (nnth 17) .array)
;; [:n33, 33, :n782, 782, :n809, 809]
(-> hbn2 (nnth 17) .array)
;; [:n33, 33, :n782, 782, :n1336, 1336, :n809, 809]

;; Notice that the :n809 entry has moved. Its location can still be found because
;; the location calculation uses the node's bitmap, and this was also changed with the
;; insertion.
(Integer/toString (-> hbn (nnth 17) .bitmap) 2)
;; "10000000001000000001000"
(Integer/toString (-> hbn2 (nnth 17) .bitmap) 2)
;; "10000001001000000001000"

;; These different bitmaps are what gives us the different positions for :n809
(pos (nnth hbn 17) :n809 10)
;; 2
(pos (nnth hbn2 17) :n809 10)
;; 3

;; Of course, now the :n1336 entry is resolved to position 2
(pos (nnth hbn2 17) :n1336 10)
;; 2
