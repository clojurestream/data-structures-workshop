;; Let's use arrays as a basic structure to build binary trees, and then balanced binary trees with red/black trees.

;; Recall the pairs created when we introduced arrays. Let's update this to refer to the first cell as the "left"
;; and the second as the "right". We can also include a piece of data, and add an extra arity so we don't have
;; to include a left and right if they're not needed.
(defn node
  ([data] (node data nil nil))
  ([data a b]
   (let [p (object-array 3)]
     (aset p 0 data)
     (aset p 1 a)
     (aset p 2 b)
     p)))

;; Let's add some convenience functions:
(defn set-child!
  [p side data]
  (aset p ({:left 1 :right 2} side) data)
  p)

(defn data [p] (nth p 0))
(defn child [p side] (nth p ({:left 1 :right 2} side)))
(defn left [p] (child p :left))
(defn right [p] (child p :right))

;; Note that setting a side that is not :left or :right will be an invalid side, causing an error, which is
;; appropriate since we don't have any other sides. Of course, many trees have more children per node, and
;; in these cases the children will typically by numbered, but for this case, `left` and `right` provides
;; an intuitive description.

;; This now gives us everything we need to create a tree of data easily. Let's create a simple tree of:
;; file:../../images/trees.html#simple
(def bottom-left (node 1))
(def bottom-right (node 9))
(def top (node 5))
(set-child! top :left bottom-left)
(set-child! top :right bottom-right)

;; We don't need to save the bottom nodes in the tree, so we can construct this in one step:
(def short-tree (node 5 (node 1) (node 9)))

;; Trees with larger branching factors save information at each node to indicate what is "between" their child nodes.
;; For instance, if there are 3 children to a node (child numbers 0, 1 and 2), then 2 values will be stored:
;; - in between the `0` and `1` nodes
;; - in between the `1` and `2` nodes
;; Navigating down a tree like this is similar to binary trees, but this time there are 3 paths to choose from
;; instead of 2.

;; But why would we be interested in a tree structure for data? In this case, we can see that the middle value is at
;; the top. All data that is smaller than the top goes to the left, and all data that is larger is on the right.
;; This means that we can quickly find is something is in the tree by comparing nodes, and going down to the left
;; or right, depending on comparison.

;; This also works in a normal array with sorted elements:
;; `[1, 5, 9]`
;; If we go to the middle, we will see the middle value. Traveling left, we see smaller values, and traveling right
;; we see larger values. So why would we want to use a more complex tree structure to accomplish the same thing?
;; The answer is when we want to insert new data. If we want to insert 3 into the array we have to move from:
;; `[1 5 9]`
;; to a larger array, with some of the elements moved to the right:
;; `[1 3 5 9]`

;; That's a lot of moving around. Instead, in a tree, we can go down the tree according the less-that-left,
;; and greater-than-right rules, and just add a new node with the data. Starting with the top, we can see that 3
;; is smaller than 5, so go down to the left. 3 is then larger than 1, so go to the right.
;; file:../../images/trees.html#insert3

;; This can be done by adding to `bottom-left`, but we need a way to insert in the correct place automatically.

;; We will eventually work with immutable data, but for the moment, let's consider mutable structures.
;; A new tree is just a mutable reference to the root of the tree. An empty tree has a reference that refers to nil.

(defn new-tree
  []
  (object-array 1))

(defn root
  [tree]
  (nth tree 0))

(defn set-root!
  [tree node]
  (aset tree 0 node)
  tree)

;; #### Exercise:
;; Write an operation (defn put! [tree value] ...) to add a new value to a leaf position in a tree.
;; Do not add the value if it already exists.
;; `tree` is a single array value that refers to the root node of the tree. That node may be `nil`.
;; `value` is a number and can be compared with <, >, and =.
;; Returns the mutated tree. This object is the same as the incoming `tree` parameter.
;; Try to recreate the tree above. The output will look like:

[[5, [1, nil, [3, nil, nil]], [9, nil, nil]]]


;; A Solution:
(defn put!
  [tree value]
  (if-not (root tree)
    ;; if empty, create a new tree with just one node
    (set-root! tree (node value))
    ;; iterate down the branches
    (loop [current-node (root tree)]
      ;; read the number at the current node
      (let [current-value (data current-node)]
        (if (= current-value value)
          ;; when equal to the number being inserted, then do nothing and return.
          tree
          ;; determine the side. Smaller goes to the left, larger to the right.
          (let [side (if (< value current-value) :left :right)]
            (if-let [child-node (child current-node side)]
              ;; if a child node exists on that side, step down and repeat
              (recur child-node)
              ;; no child on the selected side, so create a leaf node and insert it
              (do
                (set-child! current-node side (node value))
                ;; return the root of the tree
                tree))))))))

;; Try it out...

(def t (new-tree))
(put! t 5)
(put! t 1)
(put! t 9)
(put! t 3)

;; With an appropriately built tree, we know that the values represented by any branch can be turned into an ordered
;; sequence by concattenating three sequences: The sequence represented by the left child, a singleton sequence of
;; the value in the node, and the sequence represented by the right child.


;; #### Exercise:
;; Write a `t-seq` function that will emit a sequence of the contents of the tree. Hint: The tree object holds
;; the root node of the tree. Write another `as-seq` function that will convert the root node into the seq.
;; Calling `(t-seq t)` should return `(1 3 5 9)`


;; A Solution:
(defn as-seq
  [node]
  (and node
       (concat (as-seq (left node))
               [(data node)]
               (as-seq (right node))))) 

(defn t-seq
  [tree]
  (as-seq (root tree))) 

(t-seq t)

;; ## Mutation
;; These operations are mutating the tree, which is not what we want in a functional language. We could continue
;; this way but this is not appropriate at all. The approach to avoiding mutation with arrays is typically to
;; set the values upon creation, and never again.

;; One useful feature of immutability of data is that the tree no longer has to change its root pointer all
;; the time. Instead every update operation leads to an entirely new tree, leaving the old tree in place.
;; Only the modified nodes are copied, along with the parent nodes to the root of the tree. This is typically
;; a short path, and results in easy and fast changes. This approach is applicable to trees of any branching
;; factor. In fact, it works even better for a high branching factor, since only the path from a node to the
;; root is copied, and the length of this path is almost always shorter for a higher branching factor.

;; Applying this to the above insertion of 3 into a tree of 1/5/9 leads to an insertion into the 1 node, which
;; causes a copy. This then becomes the child of the 5 node, which also leads to a copied node. The new 5 node
;; will be the new root of the tree:
;; file:../../images/trees.html#immutable

;; Both the 5 and the 5' nodes represent full trees. They both share a 9 node as a right child. However, they have
;; different nodes on the left: 5 has 1, and 5' has 1'. The 1' node is the only node with a 3 as a child.

;; In some ways this is actually easier to put together. `nil` can represent the empty tree, and a new tree can be
;; returned from `put`. Nodes will only ever be created, and will never be changed. Also, because every node above
;; the current node will have to be created, the path down to a node need to be remembered. This is done
;; automatically on the stack when we use recursion instead of a loop.

(defn put
  [current-node value]
  ;; check if there is a current node
  (if-not current-node
    ;; no node, meaning this is a leaf position (includes root for an empty tree)
    (node value)
    ;; read the data in the current node
    (let [current-value (data current-node)]
      (if (= current-value value)
        ;; when the value to be inserted already exists, just return the current node
        current-node
        ;; check to see if larger or smaller
        (if (< value current-value)
          ;; smaller, so put into the left child branch
          (node current-value
                (put (left current-node) value)
                (right current-node))
          ;; larger, so put into the right child branch
          (node current-value
                (left current-node)
                (put (right current-node) value))))))) 

;; Now we create a tree by updating the last returned value, over and over:
(put (put (put nil 5) 1) 9)

;; Rather than nesting, we can move to the threading macro:
(-> nil
    (put 5)
    (put 1)
    (put 9)) 

;; #### Exercise:
;; Write a function `utree` that creates a tree from a seq parameter.

;; A solution:
(defn utree [s] (reduce put nil s))

;; #### Exercise:
;; Write a function called `tcontains?` which checks if a value has been stored in the tree. This is the same operation
;; as `clojure.core/contains?`

;; A solution:
(defn tcontains?
  [node value]
  (and node   ;; test if the tree rooted at `node` is not empty
       ;; look at the data for the current node
       (let [v (data node)]
         (or
           ;; if the data node data is the same as what is being looked for, then return true
          (= value v)
          ;; otherwise, recurse into the left child branch if the value is smaller, or the right if larger
          (recur (if (< value v) (left n) (right node)))))))

(def t (utree [5 1 9]))
(tcontains? t 1)
(tcontains? t 2)

;; ## Balancing
;; 
;; We have been looking at a tree that happened to be populated efficiently. However, if a simple tree like this is
;; populated in the wrong way, then it can be unbalanced. For instance, if the data is already sorted, then each
;; new value to be inserted will be larger than what came before, and therefore always be inserted to the right.
(-> nil
    (put 1)
    (put 3)
    (put 5)
    (put 9)) 

;; This looks like the following:
;; file:../../images/trees.html#list

;; This is just a linked list, and is very expensive to search. So keeping the tree balanced is very important.
;; The insertion order cannot be predicted in most cases, so instead the tree must be balanced during insertion.
;; There are several approaches for this, depending on the tree type, and the branching factor.
;; The one adopted by Clojure for binary trees is the Red/Black tree.

;; Red/Black trees have an extra value associated with each node: the color. This is either red or black.
;; All nodes being inserted start out as red.
(defn rb-node
  ([data] (node data nil nil :red))
  ([data a b color]
   (let [p (object-array 4)]
     (aset p 0 data)
     (aset p 1 a)
     (aset p 2 b)
     (aset p 3 color)
     p)))

(defn color [rb-node] (and rb-node (nth rb-node 3)))

;; The array positions for the value and children are the same as for the previous nodes, so these functions
;;can be reused.

;; Inserting into a red/black tree has a few peculiarities. Each node being inserted will be :red, unless it
;; is the root of the tree. The root of the tree will always be black. The other important element of
;; red/black trees is balancing the branches. We will define that in a moment:
(declare balance)

(defn rb-put
  [tree d]
  ;; create an insertion operation for a tree based on a node
  (letfn [(insert [node]
            (if-not node
              ;; an empty sub-tree means this is a leaf and the red-black node is created here
              (rb-node d nil nil :red)
              ;; extract all the parts of the node: data, left branch, right branch, and color
              (let [v (data node)
                    l (left node)
                    r (right node)
                    c (color node)]
                (cond
                  ;; smaller than the current node, so insert in the left child branch, then balance
                  (< d v) (balance (rb-node v (insert l) r c))
                  ;; larger than the current node, so insert in the right child branch, then balance
                  (> d v) (balance (rb-node v l (insert r) c))
                  ;; equal to the value in the current node, so nothing need be inserted
                  :equal node))))]
    ;; insert into the root of the tree
    (let [root (insert tree)
          v (data root)
          l (left root)
          r (right root)]
      ;; recreate the root node with the color set to black
      (rb-node v l r :black)))) 

;; Balancing gets a little complex, and typically involves updating grandparent nodes when a node is rebalanced.
;; Fortunately, unwinding the stack as the `balance` function returns removes a lot of the complexity found when
;; doing this with mutation. Were mutation being used, then some unnecessary changing of references can be
;; avoided, but these are being copied anyway, so skipping those steps does not help. This results in
;; slightly more operations, but has the benefit of much simpler operations.
;; Again, notice how mutation is avoided. Instead, nodes are read, and then entirely new nodes are created
;; with the appropriate values.
;; TODO: Diagram from Okasaki
(defn balance
  [node]
  (let [value (data node)
        l (left node)
        r (right node)
        clr (color node)]
    (if-let [[y x a b z c d]
             (and
              (= :black clr)
              (if (= :red (color l))
                (and
                 l
                 (let [left-left (left l)]
                   (if (= :red (color left-left))
                     [(data l) (data left-left) (left left-left) (right left-left) value (right l) r]
                     (let [right-left (right l)]
                       (and
                        (= :red (color right-left))
                        [(data right-left) (data l) left-left (left right-left) value (right right-left) r])))))
                (and
                 (= :red (color r))
                 (let [left-right (left r)
                       a l
                       x value]
                   (if (= :red (color left-right))
                     [(data left-right) value l (left left-right) (data r) (right left-right) (right r)]
                     (let [right-right (right r)]
                       (and
                        (= :red (color right-right))
                        [(data r) value l left-right (data right-right) (left right-right) (right right-right)])))))))]
      (rb-node y (rb-node x a b :black) (rb-node z c d :black) :red)
      node))) 

;; Just like the previous `utree` function, we can use `reduce` with `rb-put` to populate a tree with a seq:
(defn tree [s] (reduce rb-put nil s))

;; We can now create a balanced tree, with sorted data. Converting to a seq can be done with the `as-seq` function,
;; as this accepts just the root node of a tree:
(-> (range 20)
    shuffle
    tree
    as-seq)


;; We can get a sense of how balanced the tree is by checking the maximum "depth" of the tree. This is measured
;; as the number of nodes in the longest path of nodes from the root to the leaf.

;; #### Exercise:
;; Write a `max-depth` function to return the maximum depth of the rb-tree. Hint: use recursion.

;; A Solution:
(defn max-depth
  [n]
  (if n
    (inc (max (max-depth (left n)) (max-depth (right n))))
    0))

;; The maximum depth can indicate that a tree is relatively balanced. A perfectly balanced tree
;; ought to be about the log2 of the number of items. Red/Black trees do not balance perfectly, but we should
;; see something up to about twice the log2 of the nodes:
;; Remembering that sorted data is very likely to make a tree unbalanced, we can insert 2^13=8192 nodes and check
;; if the depth is somewhere close to 13:
(max-depth (tree (range 8192)))

;; ## Maps
;; As we saw with the node color, node arrays can contain extra data with no problems. Consequently, we can associate
;; any value with a node, by adding one more space to the node array. However, this would affect the put and balance
;; operations. So instead of that, let's update the data to contain 2 values in a pair. Remember that a pair
;; is just a short, 2 element array.

(defn kv-node
  ([key value] (kv-node key value nil nil :red))
  ([key value a b color]
   (let [p (object-array 4)]
     (aset p 0 (pair key value))
     (aset p 1 a)
     (aset p 2 b)
     (aset p 3 color)
     p)))

(defn nkey [kv-node] (and kv-node (nth (nth kv-node 0) 0)))
(defn value [kv-node] (and kv-node (nth (nth kv-node 0) 1)))

;; We can still search and compare in the tree according to the first value, which we are calling the nkey. The second
;; is never compared, but gets stored alongside the nkey. This becomes a key/value pair, and it's how maps work.

;; Another refinement we can make is to update the comparison operation from the numeric < and > to use
;; the `compare` function instead. This will allow a key value for anything that can be compared, not just numbers.
(defn kv-put
  [tree key val]
  (letfn [(insert [node]
            (if-not node
              ;; create a red node to contain the entry at a leaf
              (kv-node key val nil nil :red)
              ;; extract all the fields in the current node
              (let [k (nkey node)
                    v (value node)
                    l (left node)
                    r (right node)
                    c (color node)
                    cmp (compare key k)]
                ;; insert to the left for smaller, right for larger, or change nothing if equal to the current node
                (cond
                  (< cmp 0) (balance (kv-node k v (insert l) r c))
                  (> cmp 0) (balance (kv-node k v l (insert r) c))
                  :equal node))))]
    ;; insert into the tree, then read the root of the tree
    (let [root (insert tree)
          k (nkey root)
          v (value root)
          l (left root)
          r (right root)]
      ;; create an identical root, but set to black
      (kv-node k v l r :black)))) 

(-> nil (kv-put :one 1) (kv-put :two 2) (kv-put :three 3))

;; #### Exercise:
;; Write `tget`: a function that adds to the `tcontains?` function to search for a given key,
;; returning the associated value.

;; A solution:
(defn tget
  [node key]
  ;; return nil for an empty tree
  (and node
       ;; compare the value at this node to the key being searched for
       (let [c (compare key (nkey node))]
         (if (zero? c)
           ;; when the node contains the required key, return the associated value
           (value node)
           ;; key not at this node, so look in the children, going left for smaller, right for larger.
           (recur (if (< c 0) (left node) (right node)))))))

;; The following map of labels to numbers can be used to demonstrate this function:

(def numbers (-> nil
                 (kv-put "one" 1)
                 (kv-put "two" 2)
                 (kv-put "three" 3)
                 (kv-put "four" 4)
                 (kv-put "five" 5)))

(tget numbers "four")
(tget numbers "two")
(tget numbers "six")

;; We have now duplicated much of the functionality of Clojure's sorted-map. This is implemented as a persistent
;; tree map, which is based on a Red/Black tree.

;; #### Exercise:
;; Implement the `tkeys` and `tvals` functions that return the keys and values of this tree set as seqs.
;; Hint: you can use the `as-seq` from above.

;; A Solution:
(defn tkeys [tree] (map first (as-seq tree)))
(defn tvals [tree] (map second (as-seq tree)))

(tkeys numbers)
(tvals numbers)

;; Remember that in the Java memory model, the string does not appear twice in the array. Instead,
;; _reference_ to the string is stored twice. This map of objects to themselves is the approach
;; that Java uses for implementing Hashsets.

;; Now that we know about immutable trees, we can build Vectors
