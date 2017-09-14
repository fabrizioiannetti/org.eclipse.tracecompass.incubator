/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.callstack.core.tests.stubs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.IFlameChartProvider;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.CallStackSeries;
import org.eclipse.tracecompass.incubator.callstack.core.instrumented.statesystem.InstrumentedCallStackAnalysis;
import org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;

import com.google.common.collect.ImmutableList;

/**
 * A stub callgraph analysis, using a state system fixture not necessarily built
 * from the {@link InstrumentedCallStackAnalysis} base class. It allows to specify the
 * callstack analysis from a state system and the patterns to use for each
 * grouping level of the callstack.
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public class CallGraphAnalysisStub extends CallGraphAnalysis {

    /**
     * The first level of the hierarchy
     */
    public static final String PROCESS_PATH = "Processes";
    /**
     * The second level of the callstack hierarchy
     */
    public static final String THREAD_PATH = "Thread";
    /**
     * The third level of the callstack hierarchy
     */
    public static final String CALLSTACK_PATH = "CallStack";
    private static final String[] PP = { PROCESS_PATH };
    private static final String[] TP = { THREAD_PATH };

    private static final List<String[]> PATTERNS = ImmutableList.of(PP, TP);

    private static class CSAnalysis extends InstrumentedCallStackAnalysis {

        private final ITmfStateSystem fSs;
        private final @Nullable List<String[]> fPatterns;

        public CSAnalysis(ITmfStateSystem fixture) {
            fSs = fixture;
            fPatterns = null;
        }

        public CSAnalysis(ITmfStateSystem fixture, List<String[]> patterns) {
            fSs = fixture;
            fPatterns = patterns;
        }

        @Override
        public @Nullable ITmfStateSystem getStateSystem() {
            return fSs;
        }

        @Override
        public Collection<CallStackSeries> getCallStackSeries() {
            List<String @NonNull []> patterns = fPatterns;
            return Collections.singleton(new CallStackSeries(fSs, patterns == null ? PATTERNS : patterns, 0, "", getHostId(), new CallStackSeries.AttributeValueThreadResolver(1))); //$NON-NLS-1$
        }

        @Override
        protected @NonNull ITmfStateProvider createStateProvider() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String[]> getPatterns() {
            return super.getPatterns();
        }

    }

    private final IFlameChartProvider fCsProvider;

    /**
     * Constructor
     *
     * @param fixture
     *            The state system to use for this analysis
     */
    public CallGraphAnalysisStub(ITmfStateSystemBuilder fixture) {
        this(new CSAnalysis(fixture));
    }

    /**
     * Constructor with patterns
     *
     * @param fixture
     *            The state system to use for this analysis
     * @param patterns
     *            The patterns to each level of the callstack hierarchy
     */
    public CallGraphAnalysisStub(ITmfStateSystemBuilder fixture, List<String[]> patterns) {
        this(new CSAnalysis(fixture, patterns));
    }

    private CallGraphAnalysisStub(IFlameChartProvider csp) {
        super(csp);
        fCsProvider = csp;
    }

    /**
     * Will trigger the iteration over the callstack series
     *
     * @return The return value of the iteration
     */
    public boolean iterate() {
        return iterateOverCallstackSerie(fCsProvider.getCallStackSeries().iterator().next(), ModelManager.getModelFor(""), new NullProgressMonitor());
    }

    @Override
    public void dispose() {
        super.dispose();
        fCsProvider.dispose();
    }

}