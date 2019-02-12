(ns grafeo.examples
  #?(:cljs (:require-macros [grafeo.examples :refer [defexample]])))

(defonce registry (atom {}))

(defn all
  "Returns all examples."
  []
  (vals @registry))

#?(:clj (defmacro defexample [example-sym string form]
          (assert (symbol? example-sym))
          (assert (string? string))
          (assert (seq form))
          `(do (def ~example-sym {:name ~(name example-sym) :form ~form :string ~string})
               (swap! grafeo.examples/registry assoc ~(keyword example-sym) ~example-sym)
               nil)))

;; Aliases

(defexample aliases
  "{
     empireHero: hero(episode: EMPIRE) {
       name
     }
     jediHero: hero(episode: JEDI) {
       name
     }
   }"
  '(((empireHero hero)
     [(episode EMPIRE)]
     name)
    ((jediHero hero)
     [(episode JEDI)]
     name)))

;; Arguments

(defexample arguments-field
  "{
     human(id: \"1000\") {
       name
       height(unit: FOOT)
     }
   }"
  '((human
     [(id "1000")]
     name
     (height [(unit FOOT)]))))

(defexample arguments-selection-set
  "{
     human(id: \"1000\") {
       name
       height
     }
   }"
  '((human
     [(id "1000")]
     name
     height)))

(defexample arguments-type
  "type Starship {
     id: ID!
     name: String!
     length(unit: LengthUnit = METER): Float
   }"
  '((type
     Starship
     (id ID!)
     (name String!)
     (length [(unit LengthUnit METER)] Float))))

;; Default variables

(defexample default-variables
  "query HeroNameAndFriends($episode: Episode = JEDI) {
     hero(episode: $episode) {
       name
       friends {
         name
       }
     }
   }"
  '((query
     HeroNameAndFriends
     [($episode Episode JEDI)]
     (hero
      [(episode $episode)]
      name
      (friends name)))))

;; Directives

(defexample directives
  "query Hero($episode: Episode, $withFriends: Boolean!) {
     hero(episode: $episode) {
       name
       friends @include(if: $withFriends) {
         name
       }
     }
   }"
  '((query
     Hero
     [($episode Episode)
      ($withFriends Boolean!)]
     (hero
      [(episode $episode)]
      name
      (friends
       ((include [(if $withFriends)]))
       name)))))

(defexample directives-flat
  "{
     human {
       name @client
       height @client
     }
   }"
  '((human
     (name ((client)))
     (height ((client))))))

;; Enums

(defexample enums
  "enum Direction {
     NORTH
     EAST
     SOUTH
     WEST
   }"
  '((enum
     Direction
     (NORTH)
     (EAST)
     (SOUTH)
     (WEST))))

(defexample enum-extension
  "extend enum Direction {
     NORTH_EAST
   }"
  '((extend-enum
     Direction
     (NORTH_EAST))))

;; Fields

(defexample field-simple
  "{
     hero {
       name
     }
   }"
  '((hero name)))

(defexample field-object
  "{
     hero {
       name
       # Queries can have comments!
       friends {
         name
       }
     }
   }"
  '((hero
     name
     (friends name))))

(defexample hero-name-and-friends
  "query HeroNameAndFriends {
     hero {
       name
       friends {
         name
       }
     }
   }"
  '((query
     HeroNameAndFriends
     (hero
      name
      (friends name)))))

;; Fragments

(defexample fragments
  "{
     leftComparison: hero(episode: EMPIRE) {
       ...comparisonFields
     }
     rightComparison: hero(episode: JEDI) {
       ...comparisonFields
     }
   }

   fragment comparisonFields on Character {
     name
     appearsIn
     friends {
       name
     }
   }"
  '(((leftComparison hero)
     [(episode EMPIRE)]
     (... comparisonFields))
    ((rightComparison hero)
     [(episode JEDI)]
     (... comparisonFields))
    (fragment
     comparisonFields (on Character)
     name
     appearsIn
     (friends name))))

(defexample fragments-variables
  "query HeroComparison($first: Int = 3) {
     leftComparison: hero(episode: EMPIRE) {
       ...comparisonFields
     }
     rightComparison: hero(episode: JEDI) {
       ...comparisonFields
     }
   }

   fragment comparisonFields on Character {
     name
     friendsConnection(first: $first) {
       totalCount
       edges {
         node {
           name
         }
       }
     }
   }"
  '((query
     HeroComparison
     [($first Int 3)]
     ((leftComparison hero)
      [(episode EMPIRE)]
      (... comparisonFields))
     ((rightComparison hero)
      [(episode JEDI)]
      (... comparisonFields)))
    (fragment
     comparisonFields (on Character)
     name
     (friendsConnection
      [(first $first)]
      totalCount
      (edges (node name))))))

;; Interface Type

(defexample interface-definition
  "interface NamedEntity {
     name: String
   }"
  '((interface
     NamedEntity
     (name String))))

(defexample interface-implements
  "type Person implements NamedEntity {
     name: String
     age: Int
   }"
  '((type
     Person
     (implements NamedEntity)
     (name String)
     (age Int))))

;; Interface Type Extension

(defexample interface-type-extension
  "extend interface NamedEntity {
     nickname: String
   }"
  '((extend-interface
     NamedEntity
     (nickname String))))

;; Inline Fragments

(defexample inline-fragments
  "query HeroForEpisode($ep: Episode!) {
     hero(episode: $ep) {
       name
       ... on Droid {
         primaryFunction
       }
       ... on Human {
         height
       }
     }
   }"
  '((query
     HeroForEpisode
     [($ep Episode!)]
     (hero
      [(episode $ep)]
      name
      ((... (on Droid))
       primaryFunction)
      ((... (on Human))
       height)))))

;; Input Values

(defexample input-values
  "{
     int_value(input: 1)
     float_value(input: 1.2)
     boolean_value(input: true)
     string_value(input: \"x\")
     null_value(input: null)
     enum_value(input: ENUM_X)
     list_value(input: [1, 2, 3])
     object_value(input: {a: 1, b: 2})
     object_nested_value(input: {a: 1, b: {c: 2} })
   }"
  '((int_value [(input 1)])
    (float_value [(input 1.2)])
    (boolean_value [(input true)])
    (string_value [(input "x")])
    (null_value [(input nil)])
    (enum_value [(input ENUM_X)])
    (list_value [(input [1 2 3])])
    (object_value [(input {a 1 b 2})])
    (object_nested_value [(input {:a 1 :b {:c 2}})])))

;; Input Objects

(defexample input-object-type-definition
  "input Point2D {
     x: Float
     y: Float
   }"
  '((input
     Point2D
     (x Float)
     (y Float))))

(defexample input-object-type-extension
  "extend input NamedEntity {
     nickname: String
   }"
  '((extend-input
     NamedEntity
     (nickname String))))

;; Meta Fields

(defexample meta-fields
  "{
     search(text: \"an\") {
       __typename
       ... on Human {
         name
       }
       ... on Droid {
         name
       }
       ... on Starship {
         name
       }
     }
   }"
  '((query
     (search
      [(text "an")]
      __typename
      ((... (on Human))
       name)
      ((... (on Droid))
       name)
      ((... (on Starship))
       name)))))

;; Mutations

(defexample mutation
  "mutation CreateReviewForEpisode($ep: Episode!, $review: ReviewInput!) {
     createReview(episode: $ep, review: $review) {
       stars
       commentary
     }
   }"
  '((mutation
     CreateReviewForEpisode
     [($ep Episode!)
      ($review ReviewInput!)]
     (createReview
      [(episode $ep)
       (review $review)]
      stars
      commentary))))

;; Object Type Extension

(defexample object-type-definition
  "type Person implements NamedEntity {
     name: String
     age: Int
   }"
  '((type
     Person
     (implements NamedEntity)
     (name String)
     (age Int))))

(defexample object-type-extension
  "extend type Story {
     isHiddenLocally: Boolean
   }"
  '((extend-type Story
      (isHiddenLocally Boolean))))

(defexample extend-query-type
  "extend type Query {
     findDog(complex: ComplexInput): Dog
     booleanList(booleanListArg: [Boolean!]): Boolean
   }"
  '((extend-type Query
      (findDog [(complex ComplexInput)] Dog)
      (booleanList [(booleanListArg [Boolean!])] Boolean))))

(defexample extend-multiple-types
  "extend type Query {
     isLoggedIn: Boolean!
     cartItems: [Launch]!
   }

   extend type Launch {
     isInCart: Boolean!
   }

   extend type Mutation {
     addOrRemoveFromCart(id: ID!): [Launch]
   }"
  '((extend-type Query
      (isLoggedIn Boolean!)
      (cartItems (Launch)))
    (extend-type Launch
      (isInCart Boolean!))
    (extend-type Mutation
      (addOrRemoveFromCart [(id ID!)] [Launch]))))

;; Operation name

(defexample operation-name
  "query HeroNameAndFriends {
     hero {
       name
       friends {
         name
       }
     }
   }"
  '((query
     HeroNameAndFriends
     (hero
      name
      (friends name)))))

;; Scalars

(defexample scalars
  "scalar Time
   scalar Url"
  '((scalar Time)
    (scalar Url)))

(defexample scalar-type-extension
  "extend scalar Url @example"
  '((extend-scalar
     Url ((example)))))

;; Schema Definitions

(defexample schema-definition
  "schema {
     query: MyQueryRootType
     mutation: MyMutationRootType
   }"
  '((schema
     (query MyQueryRootType)
     (mutation MyMutationRootType))))

(defexample schema-extension
  "extend schema {
     query: MyQueryRootType
     mutation: MyMutationRootType
   }"
  '((extend-schema
     (query MyQueryRootType)
     (mutation MyMutationRootType))))

(defexample schema-definitions
  "schema {
     query: MyQueryRootType
     mutation: MyMutationRootType
   }

   type MyQueryRootType {
     someField: String
   }

   type MyMutationRootType {
     setSomeField(to: String): String
   }"
  '((schema
     (query MyQueryRootType)
     (mutation MyMutationRootType))
    (type
     MyQueryRootType
     (someField String))
    (type
     MyMutationRootType
     (setSomeField [(to String)] String))))

;; Unions

(defexample union-search-result
  "union SearchResult = Photo | Person"
  '((union SearchResult (Photo Person))))

(defexample union-type-extension
  "extend union SearchResult = Photo | Person"
  '((extend-union SearchResult (Photo Person))))

;; Variables

(defexample variables
  "query HeroNameAndFriends($episode: Episode) {
     hero(episode: $episode) {
       name
       friends {
         name
       }
     }
   }"
  '((query
     HeroNameAndFriends
     [($episode Episode)]
     (hero
      [(episode $episode)]
      name
      (friends name)))))

(defexample variable-types
  "query Types(
     $boolean: Boolean,
     $enum: MY_ENUM,
     $float: Float,
     $int: Int,
     $list_type: [Int],
     $object: MyObject,
     $string: String
   ) {
     types(
       boolean: $boolean,
       enum: $enum,
       float: $float,
       int: $int,
       list_type: $list_type,
       object: $object,
       string: $string
     ) {
       id
     }
   }"
  '((query
     Types
     [($boolean Boolean)
      ($enum MY_ENUM)
      ($float Float)
      ($int Int)
      ($list_type [Int])
      ($object MyObject)
      ($string String)]
     (types
      [(boolean $boolean)
       (enum $enum)
       (float $float)
       (int $int)
       (list_type $list_type)
       (object $object)
       (string $string)]
      id))))

(defexample variable-types-non-null
  "query TypesNonNull(
     $boolean: Boolean!,
     $enum: MY_ENUM!,
     $float: Float!,
     $int: Int!,
     $list_type: [Int!]!,
     $object: MyObject!,
     $string: String!
   ) {
     types(
       boolean: $boolean,
       enum: $enum,
       float: $float,
       int: $int,
       list_type: $list_type,
       object: $object,
       string: $string
     ) {
       id
     }
   }"
  '((query
     TypesNonNull
     [($boolean Boolean!)
      ($enum MY_ENUM!)
      ($float Float!)
      ($int Int!)
      ($list_type (Int!))
      ($object MyObject!)
      ($string String!)]
     (types
      [(boolean $boolean)
       (enum $enum)
       (float $float)
       (int $int)
       (list_type $list_type)
       (object $object)
       (string $string)]
      id))))

(defexample variable-list-type-nullable
  "query Search($ids: [ID!]) {
     search(ids: $ids) {
       edges {
         node {
           id
         }
       }
     }
   }"
  '((query
     Search
     [($ids [ID!])]
     (search
      [(ids $ids)]
      (edges (node id ))))))

(defexample variable-list-type-non-nullable
  "query Search($ids: [ID!]!) {
     search(ids: $ids) {
       edges {
         node {
           id
         }
       }
     }
   }"
  '((query
     Search
     [($ids (ID!))]
     (search
      [(ids $ids)]
      (edges (node id ))))))
