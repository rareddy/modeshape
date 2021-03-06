/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * <p>The Query API provides a mechanism for building and executing queries.  The framework
 * provides a reusable and extensible query engine that is capable of planning, validating, optimizing, and executing queries
 * against a generic back-end system.  
 * </p>
 * <h3>Abstract query model</h3>
 * <p>
 * At the heart of the entire query system is a single representation of what constitutes a query.  The
 * {@link org.modeshape.jcr.query.model abstract query model} defines a language-independent vocabulary 
 * for queries, and consists of a family of Java classes each represent the important semantic elements
 * needed to fully define a query.
 * </p>
 * <p>
 * There are two ways to construct abstract query models.  The first is to programmatically construct a query
 * model using the {@link org.modeshape.jcr.query.QueryBuilder}, which provides a fluent API that makes it easy to create a query
 * with Java code.  The second (and more common) approach is to use a {@link org.modeshape.jcr.query.parse.QueryParser}
 * that parses a query represented in a specific language (like SQL or XPath) and then creates the query's equivalent abstract query model.
 * There's even a {@link org.modeshape.jcr.query.parse.QueryParsers} class that can manage the parsers for multiple languages.
 * </p>
 * <p>
 * The abstract query model classes are immutable, making them very easily shared or reused if that is advantageous.
 * </p>
 * <h3>SQL language</h3>
 * <p>
 * One of the {@link org.modeshape.jcr.query.parse.QueryParser} implementation provided out of the box is
 * the {@link org.modeshape.jcr.query.parse.BasicSqlQueryParser}, which understands a subset of SQL.
 * </p>
 * <h3>QueryEngine</h3>
 * <p>
 * The {@link org.modeshape.jcr.query.QueryEngine} is the component that accepts and executes queries expressed as abstract query models.
 * Each submitted query is planned, validated, optimized, and then processed to compute and return the final 
 * {@link org.modeshape.jcr.query.QueryResults query results}.
 * </p>
 * <p>
 * Note that the QueryEngine is thread-safe.
 * </p>
 * <h4>Planning</h4>
 * <p>
 * In the <i>planning</i> stage, a canonical plan is generated for each query.  This plan is a tree of {@link org.modeshape.jcr.query.plan.PlanNode}
 * objects that each represent a different aspect of the query, and is a form that is easily manipulated by subsequent stages.
 * Any implementation of {@link org.modeshape.jcr.query.plan.Planner} can be used, though a {@link org.modeshape.jcr.query.plan.CanonicalPlanner}
 * implementation is provided and will be sufficient for most cases.  In fact, the subsequent execution steps often
 * require the plan to be in its canonical form, so for most situations it may be best to simply reuse the CanonicalPlanner
 * and in other simply extend it.
 * </p>
 * <p>
 * Note that query plans are mutable and not thread-safe, meaning that such plans are not intended to be shared
 * or reused.
 * </p>
 * <h4>Optimization</h4>
 * <p>
 * In the <i>optimization</i> stage, the canonical query plan is evaluated, validated, and manipulated to produce a more
 * a single optimized query processing plan.  The query plan is often changed in situ, although this is not required
 * of the {@link org.modeshape.jcr.query.optimize.Optimizer} implementations. A library of existing 
 * {@link org.modeshape.jcr.query.optimize.OptimizerRule} classes is provided, though it's very easy to 
 * add more optimizer rules.
 * </p>
 * <p>
 * The {@link org.modeshape.jcr.query.optimize.RuleBasedOptimizer} is an implementation that optimizes a query 
 * using a stack of rules.  A new stack is created for each rule, though the rules are required to be immutable and thus
 * often shared and reused.  And, the RuleBasedOptimizer is easily subclassed to define a custom stack of rules.
 * </p>
 * <h4>Validation</h4>
 * <p>
 * The canonical planner or the optimization rules have access to the table and column definitions that may be
 * queried.  The query framework does not prescribe the semantics of a table or column, but instead provides
 * a {@link org.modeshape.jcr.query.validate.Schemata} interface that provides access to the immutable
 * {@link org.modeshape.jcr.query.validate.Schemata.Table} definitions (that then contain the
 * {@link org.modeshape.jcr.query.validate.Schemata.Column} and {@link org.modeshape.jcr.query.validate.Schemata.Key} definitions).
 * </p>
 * <p>
 * The canonical planner and a number of the provided optimizer rules use the Schemata to verify that the
 * query is referencing an existing table and columns, whatever they are defined to be.  Although any Schemata
 * implementaiton can be used, the query framework provides an {@link org.modeshape.jcr.query.validate.ImmutableSchemata}
 * class with a {@link org.modeshape.jcr.query.validate.ImmutableSchemata.Builder builder} with a fluent API
 * that can create the corresponding immutable table, column and key definitions.
 * </p>
 * <h4>Processing</h4>
 * <p>
 * In the <i>processing</i> stage, the optimized query plan is used to construct and assemble a {@link org.modeshape.jcr.query.NodeSequence} 
 * that abstractly represents the nodes that satisfy the query. When the sequence is accessed, the processing logic of the 
 * query engine dynamically performs the necessary operations to return the correct nodes.
 * </p>
 * <h3>QueryResults</h3>
 * <p>
 * A query over a graph of content will result in a set of nodes that matched the criteria specified in the query.
 * Each node contained in the results will be identified by its {@link org.modeshape.jcr.cache.NodeKey} as well
 * as any values for the selected properties. Typically, queries will result in a single node per row, although
 * joins may result in multiple rows per row.
 * </p>
 */

package org.modeshape.jcr.query;
