(ns grafeo.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [grafeo.util :as util]))

(def ^:private gen-max
  "The :gen-max parameter used when generating collections."
  3)

(defn- conform-name
  "Strip the namespace from `x`."
  [x]
  (cond
    (keyword? x)
    (keyword (name x))
    (symbol? x)
    (symbol (util/simple-class-name (name x)))
    (string? x) x
    :else ::s/invalid))

(def ^:private name-conformer
  (s/conformer conform-name))

(defn- js-int?
  "Returns true if `n` is a JavaScript integer, otherwise false."
  [n]
  (= (rem n 1) 0))

(defn- name?
  "Returns true if `x` is a GraphQL name, otherwise false."
  [x]
  (some? (re-matches #"[_a-zA-Z][_0-9a-zA-Z-]*" (name x))))

(defn- upper-case?
  "Returns true if `x` is all upper case, otherwise false."
  [x]
  (some? (re-matches #"[_0-9A-Z-]*" (name x))))

(defn- sym
  "Returns the spec for a namespace qualified or unqualified symbol."
  [type]
  (s/with-gen (s/and name-conformer #{(symbol (name type))})
    #(gen/fmap (fn [ns] (symbol (name ns) (name type))) (gen/symbol-ns))))

(defn- operation
  "Returns the spec for an operation of the given `type`."
  [type]
  (s/cat :operation-type (sym type)
         :operation-name (s/? :grafeo/operation-name)
         :variable-definitions (s/? :grafeo/variable-definitions)
         :directives :grafeo/directives?
         :selection-set :grafeo/selection-set?))

(defn- variable-name?
  "Returns true if `x` is a variable name, otherwise false."
  [x]
  (and (str/starts-with? (name x) "$")
       (name? (.substring (name x) 1))))

(s/def :grafeo/description?
  (s/? string?))

(s/def :grafeo/name
  (s/and string? name?))

(s/def :grafeo/symbol
  (s/with-gen (s/and name-conformer symbol? name?)
    #(gen/fmap symbol (s/gen :grafeo/name))))

(s/def :grafeo/upper-symbol
  (s/with-gen (s/and name-conformer symbol? name? upper-case?)
    #(gen/fmap (comp symbol str/upper-case) (s/gen :grafeo/name))))

(s/def :grafeo/type-name :grafeo/symbol)

;; Type Conditions - https://facebook.github.io/graphql/June2018/#sec-Type-Conditions

(s/def :grafeo/type-condition
  (s/cat :id (sym 'on)
         :type :grafeo/named-type))

;; Input Values - https://facebook.github.io/graphql/June2018/#sec-Input-Values

(s/def :grafeo/boolean-value boolean?)

(def ^:private enum-value-reserved?
  #{'null (symbol "false") (symbol "true") })

(s/def :grafeo/enum-value
  (s/and symbol? #(not (enum-value-reserved? %))))

(s/def :grafeo/float-value
  (s/and float? #?(:cljs (complement js-int?))))

(s/def :grafeo/int-value
  (s/and int? #?(:cljs js-int?)))

(s/def :grafeo/list-value
  (s/coll-of :grafeo/value :kind vector? :gen-max gen-max))

(s/def :grafeo/null-value nil?)
(s/def :grafeo/object-value map?)
(s/def :grafeo/string-value string?)

(s/def :grafeo/variable-value
  (s/with-gen (s/and name-conformer
                     symbol?
                     #(str/starts-with? (name %) "$")
                     #(name? (.substring (name %) 1)))
    #(gen/fmap (fn [s] (symbol (str "$" s))) (s/gen :grafeo/name))))

(s/def :grafeo/value
  (s/or :variable :grafeo/variable-value
        :boolean-value :grafeo/boolean-value
        :enum-value :grafeo/enum-value
        :int-value :grafeo/int-value
        :float-value :grafeo/float-value
        :list-value :grafeo/list-value
        :null-value :grafeo/null-value
        :object-value :grafeo/object-value
        :string-value :grafeo/string-value))

(s/def :grafeo/value?
  (s/? :grafeo/value))

;; Arguments - https://facebook.github.io/graphql/June2018/#sec-Language.Arguments

(s/def :grafeo/argument-name :grafeo/symbol)
(s/def :grafeo/argument-value :grafeo/value)

(s/def :grafeo/argument
  (s/cat :argument-name :grafeo/argument-name
         :argument-value :grafeo/argument-value))

(s/def :grafeo/arguments
  (s/coll-of :grafeo/argument :kind vector? :gen-max gen-max :min-count 1))

(s/def :grafeo/arguments?
  (s/? :grafeo/arguments))

;; Argument Definitions - https://facebook.github.io/graphql/June2018/#ArgumentsDefinition

(s/def :grafeo/argument-definitions
  (s/coll-of :grafeo/input-value-definition :kind vector? :min-count 1 :gen-max gen-max))

;; Directives - https://facebook.github.io/graphql/June2018/#sec-Language.Directives

(s/def :grafeo/directive-name :grafeo/symbol)

(s/def :grafeo/directive
  (s/cat :directive-name :grafeo/directive-name
         :arguments :grafeo/arguments?))

(s/def :grafeo/directives
  (s/coll-of :grafeo/directive :kind list? :gen-max gen-max))

(s/def :grafeo/directives?
  (s/? :grafeo/directives))

;; Enum Extension - https://facebook.github.io/graphql/June2018/#EnumTypeExtension

(s/def :grafeo/enum-type-extension
  (s/cat :id (sym 'extend-enum)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :enum-values-definition (s/? :grafeo/enum-values-definition)))

;; Enum Type Definition - https://facebook.github.io/graphql/June2018/#EnumTypeDefinition

(s/def :grafeo/enum-value-definition
  (s/cat :enum-value :grafeo/enum-value
         :description :grafeo/description?
         :directives :grafeo/directives?))

(s/def :grafeo/enum-values-definition
  (s/+ (s/spec :grafeo/enum-value-definition)))

(s/def :grafeo/enum-type-definition
  (s/cat :id (sym 'enum)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :enum-values-definition :grafeo/enum-values-definition))

;; Inline Fragments - https://facebook.github.io/graphql/June2018/#InlineFragment

(s/def :grafeo/inline-fragment-definition
  (s/cat :id (sym '...)
         :type-condition (s/spec :grafeo/type-condition)
         :directives :grafeo/directives?))

(s/def :grafeo/inline-fragment
  (s/cat :definition (s/spec :grafeo/inline-fragment-definition)
         :selection-set :grafeo/selection-set))

;; Interface Type Definition - https://facebook.github.io/graphql/June2018/#InterfaceTypeDefinition

(s/def :grafeo/interface-type-definition
  (s/cat :id (sym 'interface)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :fields-definition :grafeo/fields-definition))

;; Interface Type Extension - https://facebook.github.io/graphql/June2018/#InterfaceTypeExtension

(s/def :grafeo/interface-type-extension
  (s/cat :id (sym 'extend-interface)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :fields-definition :grafeo/fields-definition))

;; Input Object Type Definition - https://facebook.github.io/graphql/June2018/#InputObjectTypeDefinition

(s/def :grafeo/input-value-definition
  (s/cat :type-name :grafeo/type-name
         :type :grafeo/type
         :default-value :grafeo/value?
         :directives :grafeo/directives?))

(s/def :grafeo/input-fields-definition
  (s/+ (s/spec :grafeo/input-value-definition)))

(s/def :grafeo/input-object-type-definition
  (s/cat :id (sym 'input)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :field-definitions :grafeo/input-fields-definition))

;; Input Object Type Extension - https://facebook.github.io/graphql/June2018/#InputObjectTypeExtension

(s/def :grafeo/input-object-type-extension
  (s/cat :id (sym 'extend-input)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :field-definitions :grafeo/input-fields-definition))

;; Fields - https://facebook.github.io/graphql/June2018/#sec-Language.Fields

(s/def :grafeo/field-name
  (s/and :grafeo/symbol #(not= "fragment" (name %))))

(s/def :grafeo/field-alias-name
  (s/and :grafeo/symbol #(not= "..." (name %))))

(s/def :grafeo/field-alias
  (s/cat :alias-name :grafeo/field-alias-name
         :field-name :grafeo/field-name))

(s/def :grafeo/field-object
  (s/cat :field-name (s/or :name :grafeo/field-name :alias :grafeo/field-alias)
         :arguments :grafeo/arguments?
         :directives :grafeo/directives?
         :selection-set :grafeo/selection-set?))

(s/def :grafeo/field
  (s/or :field-name :grafeo/field-name
        :field-object :grafeo/field-object))

;; Fields Definition - https://facebook.github.io/graphql/June2018/#FieldsDefinition

(s/def :grafeo/field-definition
  (s/cat :field :grafeo/field
         :description :grafeo/description?
         :argument-definitions (s/? :grafeo/argument-definitions)
         :type :grafeo/type
         :directives :grafeo/directives?))

(s/def :grafeo/fields-definition
  (s/+ (s/spec :grafeo/field-definition)))

;; Fragments - https://facebook.github.io/graphql/June2018/#sec-Language.Fragments

(s/def :grafeo/fragment-name
  (s/and :grafeo/symbol #(not= "on" (name %))))

(s/def :grafeo/fragment-spread
  (s/cat :id (sym '...)
         :fragment-name :grafeo/fragment-name
         :directives :grafeo/directives?))

(s/def :grafeo/fragment-definition
  (s/cat :id (sym 'fragment)
         :fragment-name :grafeo/fragment-name
         :type-condition (s/spec :grafeo/type-condition)
         :directives :grafeo/directives?
         :selection-set :grafeo/selection-set))

(s/def :grafeo/fragment-definitions
  (s/* (s/spec :grafeo/fragment-definition)))

;; Object Type Definition - https://facebook.github.io/graphql/June2018/#ObjectTypeDefinition

(s/def :grafeo/implements-interfaces
  (s/cat :id (sym 'implements)
         :interfaces (s/* :grafeo/symbol)))

(s/def :grafeo/implements-interfaces?
  (s/? (s/spec :grafeo/implements-interfaces)))

(s/def :grafeo/object-type-definition
  (s/cat :id (sym 'type)
         :type-name :grafeo/type-name
         :implements-interfaces :grafeo/implements-interfaces?
         :directives :grafeo/directives?
         :fields-definition :grafeo/fields-definition))

;; Object Type Extension - https://facebook.github.io/graphql/June2018/#ObjectTypeExtension

(s/def :grafeo/object-type-extension
  (s/cat :id (sym 'extend-type)
         :type-name :grafeo/type-name
         :implements-interfaces :grafeo/implements-interfaces?
         :directives :grafeo/directives?
         :fields-definition :grafeo/fields-definition))

;; Operations - https://facebook.github.io/graphql/June2018/#sec-Language.Operations

(s/def :grafeo/operation-name :grafeo/symbol)

(def operation-types
  "The GraphQL operation types."
  '#{mutation subscription query})

(s/def :grafeo/operation-type
  (s/with-gen (s/and name-conformer operation-types)
    #(s/gen operation-types)))

(s/def :grafeo/operation-definition
  (s/or :mutation (operation :mutation)
        :query (operation :query)
        :subscription (operation :subscription)))

;; Operation Type Definition - https://facebook.github.io/graphql/June2018/#OperationTypeDefinition

(s/def :grafeo/operation-type-definition
  (s/cat :operation-type :grafeo/operation-type
         :named-type :grafeo/named-type))

(s/def :grafeo/operation-type-definitions
  (s/* (s/spec :grafeo/operation-type-definition)))

;; Scalars - https://facebook.github.io/graphql/June2018/#sec-Scalars

(s/def :grafeo/scalar-type-definition
  (s/cat :id (sym 'scalar)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?))

;; Scalar Type Extension - https://facebook.github.io/graphql/June2018/#ScalarTypeExtension

(s/def :grafeo/scalar-type-extension
  (s/cat :id (sym 'extend-scalar)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?))

;; Schema Extension - https://facebook.github.io/graphql/June2018/#SchemaExtension

(s/def :grafeo/schema-extension
  (s/cat :id (sym 'extend-schema)
         :directives :grafeo/directives?
         :operation-type-definitions :grafeo/operation-type-definitions))

;; Schema Definition - https://facebook.github.io/graphql/June2018/#SchemaDefinition

(s/def :grafeo/schema-definition
  (s/cat :id (sym 'schema)
         :directives :grafeo/directives?
         :operation-type-definitions :grafeo/operation-type-definitions))

;; Selection Sets - https://facebook.github.io/graphql/June2018/#sec-Selection-Sets

(s/def :grafeo/selection
  (s/or :fragment-spread :grafeo/fragment-spread
        :inline-fragment :grafeo/inline-fragment
        :field :grafeo/field))

(s/def :grafeo/selection-set
  (s/* :grafeo/selection))

(s/def :grafeo/selection-set?
  (s/? :grafeo/selection-set))

;; Types

(s/def :grafeo/named-type :grafeo/symbol)

(s/def :grafeo/named-type-non-null
  (s/with-gen (s/and symbol? #(str/ends-with? (name %) "!"))
    #(gen/fmap (fn [s] (symbol (str s "!"))) (s/gen :grafeo/symbol))))

(s/def :grafeo/non-null-type
  (s/or :named-type-non-null :grafeo/named-type-non-null
        :list-type-non-null :grafeo/list-type-non-null))

(s/def :grafeo/list-type
  (s/coll-of :grafeo/type :kind vector? :max-count 1 :min-count 1))

(s/def :grafeo/list-type-non-null
  (s/coll-of :grafeo/type :kind list? :max-count 1 :min-count 1))

(s/def :grafeo/type
  (s/or :named-type :grafeo/named-type
        :non-null-type :grafeo/non-null-type
        :list-type :grafeo/list-type))

;; Type Definition - https://facebook.github.io/graphql/June2018/#TypeDefinition

(s/def :grafeo/type-definition
  (s/or :enum-type-definition :grafeo/enum-type-definition
        :input-object-type-definition :grafeo/input-object-type-definition
        :interface-type-definition :grafeo/interface-type-definition
        :object-type-definition :grafeo/object-type-definition
        :scalar-type-definition :grafeo/scalar-type-definition
        :union-type-definition :grafeo/union-type-definition))

(s/def :grafeo/type-system-definition
  (s/or :schema-definition :grafeo/schema-definition
        :type-definition :grafeo/type-definition
        ;; TODO:
        ;; :directive-definition :grafeo/directive-definition
        ))

;; Type Extension - https://facebook.github.io/graphql/June2018/#TypeExtension

(s/def :grafeo/type-extension
  (s/or :enum-type-extension :grafeo/enum-type-extension
        :input-object-type-extension :grafeo/input-object-type-extension
        :interface-type-extension :grafeo/interface-type-extension
        :object-type-extension :grafeo/object-type-extension
        :scalar-type-extension :grafeo/scalar-type-extension
        :union-type-extension :grafeo/union-type-extension))

;; Type System Extension - https://facebook.github.io/graphql/June2018/#TypeSystemExtension

(s/def :grafeo/type-system-extension
  (s/or :type-extension :grafeo/type-extension
        :schema-extension :grafeo/schema-extension))

;; Union Type Extension - https://facebook.github.io/graphql/June2018/#UnionTypeExtension

(s/def :grafeo/union-type-extension
  (s/cat :id (sym 'extend-union)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :union-types :grafeo/union-types))

;; Union Type Definition - https://facebook.github.io/graphql/June2018/#UnionTypeDefinition

(s/def :grafeo/union-types
  (s/coll-of :grafeo/symbol :kind list? :min-count 1 :gen-max gen-max))

(s/def :grafeo/union-type-definition
  (s/cat :id (sym 'union)
         :type-name :grafeo/type-name
         :directives :grafeo/directives?
         :union-types :grafeo/union-types))

;; Variables - https://facebook.github.io/graphql/June2018/#sec-Language.Variables

(s/def :grafeo/variable
  (s/with-gen (s/and symbol? variable-name? name-conformer)
    #(gen/fmap (fn [k] (symbol (str "$" (name k)))) (s/gen :grafeo/symbol))))

(s/def :grafeo/variable-definition
  (s/cat :variable-name :grafeo/variable
         :type :grafeo/type
         :default-value :grafeo/value?))

(s/def :grafeo/variable-definitions
  (s/coll-of :grafeo/variable-definition :kind vector? :gen-max gen-max :min-count 1))

;; Document - https://facebook.github.io/graphql/June2018/#sec-Language.Document

(s/def :grafeo/executable-definition
  (s/or :operation-definition :grafeo/operation-definition
        :fragment-definition :grafeo/fragment-definition))

(s/def :grafeo/document-definition
  (s/or :executable-definition :grafeo/executable-definition
        :type-system-definition :grafeo/type-system-definition
        :type-system-extension :grafeo/type-system-extension))

(s/def :grafeo/full-document
  (s/coll-of :grafeo/document-definition :gen-max gen-max))

(s/def :grafeo/short-document
  (s/cat :selection-set :grafeo/selection-set
         :fragment-definitions :grafeo/fragment-definitions))

(s/def :grafeo/document
  (s/or :full-document :grafeo/full-document
        :short-document :grafeo/short-document))
