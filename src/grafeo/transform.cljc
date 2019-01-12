(ns grafeo.transform
  (:require [grafeo.util :as util]))

(def ^:dynamic *naming-strategies*
  "The default naming strategies."
  {:alumbra/argument-name util/underscore
   :alumbra/directive-name util/underscore
   :alumbra/enum util/underscore
   :alumbra/field-alias util/underscore
   :alumbra/field-name util/underscore
   :alumbra/fragment-name util/underscore
   :alumbra/operation-name util/underscore
   :alumbra/operation-type util/underscore
   :alumbra/type-name util/underscore
   :alumbra/variable-name util/underscore})

(def default-metadata
  "The default metadata."
  {:column 0 :row 0})

(defn- naming-strategy
  "Returns the naming strategy for `type`."
  [type]
  (get *naming-strategies* (keyword type)))

(defn- rename
  "Rename `val` according to the naming strategy for `type`."
  [type val]
  (when-let [f (naming-strategy type)]
    (f val)))

(defmulti transform
  "Transform a Grafeo Spec AST into an Alumbra AST."
  first)

(defn- make-argument
  "Returns the Alumbra AST for a GraphQL argument."
  [{:keys [argument-name argument-value]}]
  {:alumbra/argument-name (rename :alumbra/argument-name argument-name)
   :alumbra/argument-value (transform argument-value)
   :alumbra/metadata default-metadata})

(defn- make-argument-definition
  "Returns the Alumbra AST for a GraphQL argument definition."
  [{:keys [default-value type-name type]}]
  (cond-> {:alumbra/argument-name (rename :alumbra/argument-name type-name)
           :alumbra/argument-type (transform type)
           :alumbra/metadata default-metadata}
    default-value
    (assoc :alumbra/default-value (transform default-value))))

(defn- make-boolean-value
  "Returns the Alumbra AST for a GraphQL boolean value."
  [boolean-value]
  {:alumbra/boolean boolean-value
   :alumbra/metadata default-metadata
   :alumbra/value-type :boolean})

(defn- make-directive
  "Returns the Alumbra AST for a GraphQL directive."
  [{:keys [arguments directive-name]}]
  (cond-> {:alumbra/directive-name (rename :alumbra/directive-name directive-name)
           :alumbra/metadata default-metadata}
    (seq arguments)
    (assoc :alumbra/arguments (mapv make-argument arguments))))

(defn- make-enum-field
  "Returns the Alumbra AST for a GraphQL enum field."
  [{:keys [enum-value]}]
  {:alumbra/enum (rename :alumbra/enum enum-value)
   :alumbra/metadata default-metadata})

(defn- make-enum-type
  "Returns the Alumbra AST for a GraphQL enum definition."
  [{:keys [type-name directives enum-values-definition]}]
  {:alumbra/metadata default-metadata
   :alumbra/type-name (rename :alumbra/type-name type-name)
   :alumbra/enum-fields (mapv make-enum-field enum-values-definition)})

(defn- make-enum-value
  "Returns the Alumbra AST for a GraphQL enum value."
  [enum-value]
  {:alumbra/enum (rename :alumbra/enum enum-value)
   :alumbra/metadata default-metadata
   :alumbra/value-type :enum})

(defn- make-named-type
  "Returns the Alumbra AST for a GraphQL named type."
  [type-name non-null?]
  {:alumbra/metadata default-metadata
   :alumbra/non-null? non-null?
   :alumbra/type-class :named-type
   :alumbra/type-name (rename :alumbra/type-name type-name)})

(defn- make-null-value
  "Returns the Alumbra AST for a GraphQL null value."
  [null-value]
  {:alumbra/metadata default-metadata
   :alumbra/value-type :null})

(defn- make-list-type
  "Returns the Alumbra AST for a GraphQL list type."
  [list-type non-null?]
  {:alumbra/element-type (transform list-type)
   :alumbra/metadata default-metadata
   :alumbra/non-null? non-null?
   :alumbra/type-class :list-type})

(defn- make-list-value
  "Returns the Alumbra AST for a GraphQL list value."
  [list-value]
  {:alumbra/list (mapv transform list-value)
   :alumbra/metadata default-metadata
   :alumbra/value-type :list})

(defn- make-type-name
  "Returns the Alumbra AST for a GraphQL type name."
  [type-name]
  {:alumbra/metadata default-metadata
   :alumbra/type-name (rename :alumbra/type-name type-name)})

(defn- make-type-condition
  "Returns the Alumbra AST for a GraphQL type condition."
  [{:keys [type]}]
  (make-type-name type))

(defn- make-field-definition
  "Returns the Alumbra AST for a GraphQL field definition."
  [{:keys [argument-definitions field type]}]
  (cond-> (transform field)
    argument-definitions
    (assoc :alumbra/argument-definitions (mapv make-argument-definition argument-definitions))
    type
    (merge {:alumbra/type (transform type)})))

(defn- field-alias [[_ {:keys [alias-name]}]]
  (some->> alias-name (rename :alumbra/field-alias)))

(defn- make-field-object
  "Returns the Alumbra AST for a GraphQL field object."
  [{:keys [arguments directives field-name selection-set]}]
  (let [field-alias (field-alias field-name)]
    (cond-> {:alumbra/field-name (transform field-name)
             :alumbra/metadata default-metadata}
      (seq arguments)
      (assoc :alumbra/arguments (mapv make-argument arguments))
      (seq directives)
      (assoc :alumbra/directives (mapv make-directive directives))
      field-alias
      (assoc :alumbra/field-alias field-alias)
      (seq selection-set)
      (assoc :alumbra/selection-set (mapv transform selection-set)))))

(defn- make-interface
  "Returns the Alumbra AST for a GraphQL interface definition or extension."
  [{:keys [directives type-name fields-definition]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/type-name (rename :alumbra/type-name type-name)}
    directives
    (assoc :alumbra/directives (mapv make-directive directives))
    (seq fields-definition)
    (assoc :alumbra/field-definitions (mapv make-field-definition fields-definition))))

(defn- make-input-field-definition
  "Returns the Alumbra AST for a GraphQL input field definition."
  [{:keys [type-name type]}]
  (cond-> {:alumbra/field-name (rename :alumbra/field-name type-name)
           :alumbra/metadata default-metadata}
    type (merge {:alumbra/type (transform type)})))

(defn- make-input-type
  "Returns the Alumbra AST for a GraphQL input type definition."
  [{:keys [type-name directives field-definitions]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/type-name (rename :alumbra/type-name type-name)}
    (seq directives)
    (assoc :alumbra/directives (mapv make-directive directives))
    (seq field-definitions)
    (assoc :alumbra/input-field-definitions (mapv make-input-field-definition field-definitions))))

(defn- make-fragment
  "Returns the Alumbra AST for a GraphQL fragment."
  [{:keys [fragment-name selection-set type-condition]}]
  {:alumbra/fragment-name (rename :alumbra/fragment-name fragment-name)
   :alumbra/metadata default-metadata
   :alumbra/selection-set (mapv transform selection-set)
   :alumbra/type-condition (make-type-condition type-condition)})

(defn- make-inline-fragment
  "Returns the Alumbra AST for a GraphQL inline fragment."
  [{:keys [definition selection-set]}]
  {:alumbra/metadata default-metadata
   :alumbra/selection-set (mapv transform selection-set)
   :alumbra/type-condition (make-type-condition (:type-condition definition))})

(defn- make-int-value
  "Returns the Alumbra AST for a GraphQL int value."
  [int-value]
  {:alumbra/integer int-value
   :alumbra/metadata default-metadata
   :alumbra/value-type :integer})

(defn- make-float-value
  "Returns the Alumbra AST for a GraphQL float value."
  [float-value]
  {:alumbra/float float-value
   :alumbra/metadata default-metadata
   :alumbra/value-type :float})

(declare make-value)

(defn- make-object-value
  "Returns the Alumbra AST for a GraphQL object value."
  [object-value]
  {:alumbra/metadata default-metadata
   :alumbra/value-type :object
   :alumbra/object
   (for [[field-name value] object-value]
     {:alumbra/field-name (rename :alumbra/field-name field-name)
      :alumbra/metadata default-metadata
      :alumbra/value (make-value value)})})

(defn- make-object-type
  "Returns the Alumbra AST for a GraphQL object definition/extension."
  [{:keys [directives type-name fields-definition implements-interfaces]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/type-name (rename :alumbra/type-name type-name)}
    directives
    (assoc :alumbra/directives (mapv make-directive directives))
    (seq fields-definition)
    (assoc :alumbra/field-definitions (mapv make-field-definition fields-definition))
    implements-interfaces
    (assoc :alumbra/interface-types (mapv make-type-name (:interfaces implements-interfaces)))))

(declare make-variable)

(defn- make-operation
  "Returns the Alumbra AST for a GraphQL operation."
  [{:keys [operation-name operation-type selection-set variable-definitions]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/operation-type (rename :alumbra/operation-type operation-type)}
    operation-name
    (assoc :alumbra/operation-name (rename :alumbra/operation-name operation-name))
    selection-set
    (assoc :alumbra/selection-set (mapv transform selection-set))
    variable-definitions
    (assoc :alumbra/variables (mapv make-variable variable-definitions))))

(defn- make-scalar-type
  "Returns the Alumbra AST for a GraphQL scalar definition or extension."
  [{:keys [directives type-name]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/type-name (rename :alumbra/type-name type-name)}
    directives
    (assoc :alumbra/directives (mapv make-directive directives))))

(defn- make-schema-field
  "Returns the Alumbra AST for a GraphQL schema field."
  [{:keys [operation-type named-type]}]
  {:alumbra/metadata default-metadata
   :alumbra/operation-type (rename :alumbra/operation-type operation-type)
   :alumbra/schema-type (make-type-name named-type)})

(defn- make-schema-type
  "Returns the Alumbra AST for a GraphQL schema definition or extension."
  [{:keys [directives operation-type-definitions]}]
  (cond-> {:alumbra/metadata default-metadata}
    directives
    (assoc :alumbra/directives (mapv make-directive directives))
    (seq operation-type-definitions)
    (assoc :alumbra/schema-fields (mapv make-schema-field operation-type-definitions))))

(defn- make-short-document
  "Returns the Alumbra AST for a GraphQL short document."
  [{:keys [fragment-definitions selection-set]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/operations [(make-operation
                                 {:operation-type "query"
                                  :selection-set selection-set})]}
    fragment-definitions
    (assoc :alumbra/fragments (mapv make-fragment fragment-definitions))))

(defn- make-string-value
  "Returns the Alumbra AST for a GraphQL string value."
  [string-value]
  {:alumbra/metadata default-metadata
   :alumbra/string string-value
   :alumbra/value-type :string})

(defn- make-union
  "Returns the Alumbra AST for a GraphQL union definition or extension."
  [{:keys [type-name directives union-types]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/type-name (rename :alumbra/type-name type-name)}
    directives
    (assoc :alumbra/directives (mapv make-directive directives))
    union-types
    (assoc :alumbra/union-types (mapv make-type-name union-types))))

(defn- make-variable-value
  "Returns the Alumbra AST for a GraphQL variable value."
  [variable-name]
  {:alumbra/metadata default-metadata
   :alumbra/value-type :variable
   :alumbra/variable-name (util/strip-variable variable-name)})

(defn- make-variable
  "Returns the Alumbra AST for a GraphQL variable."
  [{:keys [default-value type variable-name]}]
  (cond-> {:alumbra/metadata default-metadata
           :alumbra/type (transform type)
           :alumbra/variable-name (util/strip-variable variable-name)}
    default-value
    (assoc :alumbra/default-value (transform default-value))))

(defn- make-value [value]
  (cond
    (boolean? value)
    (make-boolean-value value)

    (int? value)
    (make-int-value value)

    (float? value)
    (make-float-value value)

    (string? value)
    (make-string-value value)

    (map? value)
    (make-object-value value)
    :else (throw (ex-info (str "Invalid value: " value) {:value value}))))

;; Transformation

(defmethod transform :alias [[_ {:keys [field-name]}]]
  (rename :alumbra/field-name field-name))

(defmethod transform :boolean-value [[_ boolean-value]]
  (make-boolean-value boolean-value))

(defmethod transform :enum-value [[_ enum-value]]
  (make-enum-value enum-value))

(defmethod transform :enum-type-definition [[_ enum-type-definition]]
  {:alumbra/enum-definitions [(make-enum-type enum-type-definition)]
   :alumbra/metadata default-metadata})

(defmethod transform :enum-type-extension [[_ enum-type-extension]]
  {:alumbra/enum-extensions [(make-enum-type enum-type-extension)]
   :alumbra/metadata default-metadata})

(defmethod transform :executable-definition [[_ executable-definition]]
  (transform executable-definition))

(defmethod transform :field [[_ field]]
  (transform field))

(defmethod transform :field-name [[_ field-name]]
  {:alumbra/field-name (rename :alumbra/field-name field-name)
   :alumbra/metadata default-metadata})

(defmethod transform :field-object
  [[_ field-object]]
  (make-field-object field-object))

(defmethod transform :float-value [[_ float-value]]
  (make-float-value float-value))

(defmethod transform :fragment-definition [[_ fragment-definition]]
  {:alumbra/fragments [(make-fragment fragment-definition)]})

(defmethod transform :fragment-spread [[_ {:keys [fragment-name]}]]
  {:alumbra/fragment-name (rename :alumbra/fragment-name fragment-name)
   :alumbra/metadata default-metadata})

(defmethod transform :full-document [[_ definitions]]
  (assoc (apply merge-with (comp vec concat) (map transform definitions))
         :alumbra/metadata default-metadata))

(defmethod transform :inline-fragment [[_ inline-fragment]]
  (make-inline-fragment inline-fragment))

(defmethod transform :input-object-type-definition [[_ input-type-definition]]
  {:alumbra/metadata default-metadata
   :alumbra/input-type-definitions [(make-input-type input-type-definition)]})

(defmethod transform :input-object-type-extension [[_ input-type-extension]]
  {:alumbra/metadata default-metadata
   :alumbra/input-extensions [(make-input-type input-type-extension)]})

(defmethod transform :int-value [[_ int-value]]
  (make-int-value int-value))

(defmethod transform :interface-type-definition [[_ interface-definition]]
  {:alumbra/metadata default-metadata
   :alumbra/interface-definitions [(make-interface interface-definition)]})

(defmethod transform :interface-type-extension [[_ interface-extension]]
  {:alumbra/metadata default-metadata
   :alumbra/interface-extensions [(make-interface interface-extension)]})

(defmethod transform :list-type [[_ [list-type]]]
  (make-list-type list-type false))

(defmethod transform :list-type-non-null [[_ [list-type]]]
  (make-list-type list-type true))

(defmethod transform :list-value [[_ list-value]]
  (make-list-value list-value))

(defmethod transform :name [[_ field-name]]
  (rename :alumbra/field-name field-name))

(defmethod transform :named-type [[_ named-type]]
  (make-named-type named-type false))

(defmethod transform :non-null-type [[_ [_ type :as non-null-type]]]
  (if (symbol? type)
    (make-named-type (util/strip-non-null type) true)
    (transform non-null-type)))

(defmethod transform :null-value [[_ null-value]]
  (make-null-value null-value))

(defmethod transform :object-type-definition [[_ object-definition]]
  {:alumbra/metadata default-metadata
   :alumbra/type-definitions [(make-object-type object-definition)]})

(defmethod transform :object-type-extension [[_ object-extension]]
  {:alumbra/metadata default-metadata
   :alumbra/type-extensions [(make-object-type object-extension)]})

(defmethod transform :object-value [[_ object-value]]
  (make-object-value object-value))

(defmethod transform :operation-definition [[_ [_ operation]]]
  {:alumbra/metadata default-metadata
   :alumbra/operations [(make-operation operation)]})

(defmethod transform :type-definition [[_ definition]]
  (transform definition))

(defmethod transform :scalar-type-definition
  [[_ scalar-definition]]
  {:alumbra/metadata default-metadata
   :alumbra/scalar-definitions [(make-scalar-type scalar-definition)]})

(defmethod transform :scalar-type-extension
  [[_ scalar-extension]]
  {:alumbra/metadata default-metadata
   :alumbra/scalar-extensions [(make-scalar-type scalar-extension)]})

(defmethod transform :schema-definition
  [[_ schema-definition]]
  {:alumbra/metadata default-metadata
   :alumbra/schema-definitions [(make-schema-type schema-definition)]})

(defmethod transform :schema-extension
  [[_ schema-extension]]
  {:alumbra/metadata default-metadata
   :alumbra/schema-extensions [(make-schema-type schema-extension)]})

(defmethod transform :short-document [[_ short-document]]
  (make-short-document short-document))

(defmethod transform :symbol [[_ symbol-name]]
  (name symbol-name))

(defmethod transform :string-value [[_ string-value]]
  (make-string-value string-value))

(defmethod transform :type-extension [[_ type-extension]]
  (transform type-extension))

(defmethod transform :type-system-definition [[_ definition]]
  (transform definition))

(defmethod transform :type-system-extension [[_ extension]]
  (transform extension))

(defmethod transform :union-type-definition [[_ union-definition]]
  {:alumbra/metadata default-metadata
   :alumbra/union-definitions [(make-union union-definition)]})

(defmethod transform :union-type-extension [[_ union-extension]]
  {:alumbra/metadata default-metadata
   :alumbra/union-extensions [(make-union union-extension)]})

(defmethod transform :variable [[_ variable-name]]
  (make-variable-value variable-name))

(defn parse-document
  "Parse the Grafeo `s-expr` and return and Alumbra AST."
  [s-expr & [{:keys [naming-strategies]}]]
  (binding [*naming-strategies* (merge *naming-strategies* naming-strategies)]
    (transform (util/conform! :grafeo/document s-expr))))
