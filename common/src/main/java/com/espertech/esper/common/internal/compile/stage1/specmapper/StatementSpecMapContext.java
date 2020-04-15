/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.common.internal.compile.stage1.specmapper;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionForge;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclDesc;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionDeclItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ExpressionScriptProvided;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.classprovided.compiletime.ClassProvidedClasspathExtension;
import com.espertech.esper.common.internal.epl.expression.core.ExprSubstitutionNode;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolver;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceCompileTime;
import com.espertech.esper.common.internal.util.LazyAllocatedMap;

import java.util.*;

/**
 * Context for mapping a SODA statement to a statement specification, or multiple for subqueries,
 * and obtaining certain optimization information from a statement.
 */
public class StatementSpecMapContext {
    private final StatementSpecMapEnv mapEnv;
    private final ContextCompileTimeDescriptor contextCompileTimeDescriptor;

    private boolean hasPriorExpression;
    private Set<String> variableNames;
    private Map<String, ExpressionDeclItem> expressionDeclarations;
    private List<ExpressionScriptProvided> scripts;
    private LazyAllocatedMap<HashableMultiKey, AggregationMultiFunctionForge> plugInAggregations;
    private Set<ExprTableAccessNode> tableNodes = new HashSet<ExprTableAccessNode>(1);
    private List<ExprSubstitutionNode> substitutionNodes = new ArrayList<>();

    public StatementSpecMapContext(ContextCompileTimeDescriptor contextCompileTimeDescriptor,
                                   StatementSpecMapEnv mapEnv,
                                   LazyAllocatedMap<HashableMultiKey, AggregationMultiFunctionForge> plugInAggregations,
                                   List<ExpressionScriptProvided> scriptExpressions) {
        this.variableNames = new HashSet<>();
        this.mapEnv = mapEnv;
        this.contextCompileTimeDescriptor = contextCompileTimeDescriptor;
        this.plugInAggregations = plugInAggregations;
        this.scripts = scriptExpressions;
    }

    public StatementSpecMapContext(ContextCompileTimeDescriptor contextCompileTimeDescriptor,
                                   StatementSpecMapEnv mapEnv) {
        this(contextCompileTimeDescriptor, mapEnv, new LazyAllocatedMap<>(), new ArrayList<>(1));
    }

    public VariableCompileTimeResolver getVariableCompileTimeResolver() {
        return mapEnv.getVariableCompileTimeResolver();
    }

    /**
     * Returns the runtimeimport service.
     *
     * @return service
     */
    public ClasspathImportServiceCompileTime getClasspathImportService() {
        return mapEnv.getClasspathImportService();
    }

    /**
     * Returns the configuration.
     *
     * @return config
     */
    public Configuration getConfiguration() {
        return mapEnv.getConfiguration();
    }

    /**
     * Returns variables.
     *
     * @return variables
     */
    public Set<String> getVariableNames() {
        return variableNames;
    }

    public Map<String, ExpressionDeclItem> getExpressionDeclarations() {
        if (expressionDeclarations == null) {
            return Collections.emptyMap();
        }
        return expressionDeclarations;
    }

    public void addExpressionDeclaration(ExpressionDeclItem item) {
        if (expressionDeclarations == null) {
            expressionDeclarations = new HashMap<String, ExpressionDeclItem>();
        }
        expressionDeclarations.put(item.getName(), item);
    }

    public List<ExpressionScriptProvided> getScripts() {
        return scripts;
    }

    public void addScript(ExpressionScriptProvided item) {
        scripts.add(item);
    }

    public String getContextName() {
        return contextCompileTimeDescriptor == null ? null : contextCompileTimeDescriptor.getContextName();
    }

    public ExprDeclaredCompileTimeResolver getExprDeclaredCompileTimeResolver() {
        return mapEnv.getExprDeclaredCompileTimeResolver();
    }

    public TableCompileTimeResolver getTableCompileTimeResolver() {
        return mapEnv.getTableCompileTimeResolver();
    }

    public LazyAllocatedMap<HashableMultiKey, AggregationMultiFunctionForge> getPlugInAggregations() {
        return plugInAggregations;
    }

    public ContextCompileTimeDescriptor getContextCompileTimeDescriptor() {
        return contextCompileTimeDescriptor;
    }

    public Set<ExprTableAccessNode> getTableExpressions() {
        return tableNodes;
    }

    public void setHasPriorExpression() {
        this.hasPriorExpression = true;
    }

    public boolean isHasPriorExpression() {
        return hasPriorExpression;
    }

    public StatementSpecMapEnv getMapEnv() {
        return mapEnv;
    }

    public List<ExprSubstitutionNode> getSubstitutionNodes() {
        return substitutionNodes;
    }

    public boolean isAttachPatternText() {
        return mapEnv.getConfiguration().getCompiler().getByteCode().isAttachPatternEPL();
    }

    public ClassProvidedClasspathExtension getClassProvidedClasspathExtension() {
        return mapEnv.getClassProvidedClasspathExtension();
    }

    public void add(StatementSpecMapContext other) {
        getTableExpressions().addAll(other.getTableExpressions());
        getVariableNames().addAll(other.getVariableNames());
    }

    public void addExpressionDeclarations(ExpressionDeclDesc expressionDeclarations) {
        for (ExpressionDeclItem item : expressionDeclarations.getExpressions()) {
            addExpressionDeclaration(item);
        }
    }

    public void addTo(StatementSpecRaw statementSpec) {
        statementSpec.getTableExpressions().addAll(getTableExpressions());
        statementSpec.getReferencedVariables().addAll(getVariableNames());
    }
}
