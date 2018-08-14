/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.wizards;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.incubator.internal.opentracing.ui.project.handlers.SplitImportTracesOperation;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Wizard page to import a trace from Jaeger
 *
 * @author Simon Delisle
 */
public class FetchJaegerTracesWizardPage extends WizardPage {

    private static final String DEFAULT_TRACE_FOLDER_NAME = "jaegerTraces"; //$NON-NLS-1$
    private static final String DEFAULT_LOOKBACK = "43200"; //$NON-NLS-1$
    private static final String DEFAULT_LIMIT = "20"; //$NON-NLS-1$
    private static final String DEFAULT_BASE_URL = "http://localhost:16686/api"; //$NON-NLS-1$

    private static final String NANOSECONDS_PADDING = "000"; //$NON-NLS-1$
    private static final String JAEGER_TRACE_ID_KEY = "traceID"; //$NON-NLS-1$
    private static final String JAEGER_DATA_KEY = "data"; //$NON-NLS-1$
    private static final String JAEGER_SPANS_KEY = "spans"; //$NON-NLS-1$
    private static final String JAEGER_SPAN_NAME_KEY = "operationName"; //$NON-NLS-1$

    private Table fTracesTable;
    private String fTraceFolderName;
    private TmfTraceFolder fTmfTraceFolder;
    private JsonObject fJaegerJsonTrace;

    /**
     * Constructor.
     *
     * @param selection
     *            Folder selection where the traces will be imported
     */
    public FetchJaegerTracesWizardPage(IStructuredSelection selection) {
        super(Messages.FetchJaegerTracesWizardPage_wizardPageName, Messages.FetchJaegerTracesWizardPage_wizardPageName, null);
        if (selection.getFirstElement() instanceof TmfTraceFolder) {
            fTmfTraceFolder = (TmfTraceFolder) selection.getFirstElement();
        }
        updatePageCompletion();
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(GridLayoutFactory.swtDefaults().create());
        composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        Group urlConfigurationGroup = new Group(composite, SWT.NONE);
        urlConfigurationGroup.setText(Messages.FetchJaegerTracesWizardPage_jaegerConfigGroup);
        urlConfigurationGroup.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
        urlConfigurationGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        Label targetUrlLabel = new Label(urlConfigurationGroup, SWT.NONE);
        targetUrlLabel.setText(Messages.FetchJaegerTracesWizardPage_apiBaseUrlLabel);
        Text targetUrlText = new Text(urlConfigurationGroup, SWT.NONE);
        targetUrlText.setText(DEFAULT_BASE_URL);
        targetUrlText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        Label serviceLabel = new Label(urlConfigurationGroup, SWT.NONE);
        serviceLabel.setText(Messages.FetchJaegerTracesWizardPage_serviceNameLabel);
        Combo serviceCombo = new Combo(urlConfigurationGroup, SWT.DROP_DOWN);
        serviceCombo.setItems(""); //$NON-NLS-1$
        serviceCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        targetUrlText.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                if (!JaegerRestUtils.jaegerCheckConnection(targetUrlText.getText())) {
                    setPageComplete(false);
                    setErrorMessage(Messages.FetchJaegerTracesWizardPage_errorApiConnection);
                } else {
                    String[] fetchServices = JaegerRestUtils.fetchServices(targetUrlText.getText());
                    if (fetchServices != null) {
                        serviceCombo.setItems(fetchServices);
                        serviceCombo.select(0);
                    }
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                updatePageCompletion();
            }
        });

        Label traceNumberLimitLabel = new Label(urlConfigurationGroup, SWT.NONE);
        traceNumberLimitLabel.setText(Messages.FetchJaegerTracesWizardPage_nbTracesLimitLabel);
        Text traceNumberLimitText = new Text(urlConfigurationGroup, SWT.NONE);
        traceNumberLimitText.setText(DEFAULT_LIMIT);
        traceNumberLimitText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        Label traceLookBackLabel = new Label(urlConfigurationGroup, SWT.NONE);
        traceLookBackLabel.setText(Messages.FetchJaegerTracesWizardPage_loockbackLabel);
        Text traceLookBackText = new Text(urlConfigurationGroup, SWT.NONE);
        traceLookBackText.setText(DEFAULT_LOOKBACK);
        traceLookBackText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        Label traceNameLabel = new Label(urlConfigurationGroup, SWT.NONE);
        traceNameLabel.setText(Messages.FetchJaegerTracesWizardPage_traceName);
        Text traceFolderNameText = new Text(urlConfigurationGroup, SWT.NONE);
        traceFolderNameText.setText(DEFAULT_TRACE_FOLDER_NAME);
        traceFolderNameText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        fTraceFolderName = DEFAULT_TRACE_FOLDER_NAME;

        Label traceDestinationLabel = new Label(urlConfigurationGroup, SWT.NONE);
        traceDestinationLabel.setText(Messages.FetchJaegerTracesWizardPage_importDestinationLabel);
        Text traceDestinationText = new Text(urlConfigurationGroup, SWT.NONE);
        traceDestinationText.setText(fTmfTraceFolder.getPath().append(fTraceFolderName).toOSString());
        traceDestinationText.setEnabled(false);
        traceDestinationText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        traceFolderNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                fTraceFolderName = traceFolderNameText.getText();
                traceDestinationText.setText(fTmfTraceFolder.getPath().append(fTraceFolderName).toOSString());
                updatePageCompletion();
            }
        });

        Group tracesInfoGroup = new Group(composite, SWT.NONE);
        tracesInfoGroup.setText(Messages.FetchJaegerTracesWizardPage_tracesGroup);
        tracesInfoGroup.setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
        tracesInfoGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        Button fetchJagerButton = new Button(urlConfigurationGroup, SWT.PUSH);
        fetchJagerButton.setText(Messages.FetchJaegerTracesWizardPage_jaegerFetchButton);
        fetchJagerButton.setLayoutData(GridDataFactory.fillDefaults().create());

        fetchJagerButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                long currentTime = Instant.now().toEpochMilli();
                long loockBackTime = Instant.now().minusSeconds(Long.parseLong(traceLookBackText.getText())).toEpochMilli();
                String requestUrl = JaegerRestUtils.buildTracesUrl(targetUrlText.getText(), serviceCombo.getText(), Long.toString(loockBackTime) + NANOSECONDS_PADDING, Long.toString(currentTime) + NANOSECONDS_PADDING, traceNumberLimitText.getText());
                String jaegerTraces = JaegerRestUtils.fetchJaegerTraces(requestUrl);
                if (jaegerTraces == null) {
                    setPageComplete(false);
                    setErrorMessage(Messages.FetchJaegerTracesWizardPage_errorFetchTraces);
                    return;
                }
                Gson gson = new Gson();
                JsonObject tracesObject = gson.fromJson(jaegerTraces, JsonObject.class);
                JsonArray tracesArray = tracesObject.get(JAEGER_DATA_KEY).getAsJsonArray();
                fTracesTable.removeAll();
                if (tracesArray.size() > 0) {
                    for (int i = 0; i < tracesArray.size(); i++) {
                        TableItem traceItem = new TableItem(fTracesTable, SWT.NONE);
                        traceItem.setText(0, tracesArray.get(i).getAsJsonObject().get(JAEGER_TRACE_ID_KEY).getAsString());
                        JsonArray spans = tracesArray.get(i).getAsJsonObject().get(JAEGER_SPANS_KEY).getAsJsonArray();
                        traceItem.setText(1, spans.get(0).getAsJsonObject().get(JAEGER_SPAN_NAME_KEY).getAsString());
                        traceItem.setText(2, Integer.toString(spans.size()));
                        traceItem.setChecked(true);
                        for(TableColumn column : fTracesTable.getColumns()) {
                            column.pack();
                        }
                    }
                    fJaegerJsonTrace = tracesObject;
                    updatePageCompletion();
                } else {
                    setPageComplete(false);
                    setErrorMessage(Messages.FetchJaegerTracesWizardPage_errorNoTracesFound);
                }
            }
        });

        fTracesTable = new Table(tracesInfoGroup, SWT.CHECK | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        fTracesTable.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        fTracesTable.setLinesVisible(true);
        fTracesTable.setHeaderVisible(true);

        TableColumn traceIdColumn = new TableColumn(fTracesTable, SWT.NONE);
        traceIdColumn.setText(Messages.FetchJaegerTracesWizardPage_traceIdColumnName);
        traceIdColumn.pack();
        TableColumn firstSpanColumn = new TableColumn(fTracesTable, SWT.NONE);
        firstSpanColumn.setText(Messages.FetchJaegerTracesWizardPage_spanNameColumnName);
        firstSpanColumn.pack();
        TableColumn nbSpanColumn = new TableColumn(fTracesTable, SWT.NONE);
        nbSpanColumn.setText(Messages.FetchJaegerTracesWizardPage_nbSpansColumnName);
        nbSpanColumn.pack();

        fTracesTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (e.detail == SWT.CHECK) {
                    updatePageCompletion();
                }
            }
        });

        Composite selectOptionsComposite = new Composite(tracesInfoGroup, SWT.NONE);
        selectOptionsComposite.setLayout(GridLayoutFactory.fillDefaults().create());
        Button selectAllButton = new Button(selectOptionsComposite, SWT.PUSH);
        selectAllButton.setText(Messages.FetchJaegerTracesWizardPage_selectAllButton);
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (TableItem tableItem : fTracesTable.getItems()) {
                    tableItem.setChecked(true);
                }
                updatePageCompletion();
            }
        });
        Button deselectAllButton = new Button(selectOptionsComposite, SWT.PUSH);
        deselectAllButton.setText(Messages.FetchJaegerTracesWizardPage_deselectAllButton);
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (TableItem tableItem : fTracesTable.getItems()) {
                    tableItem.setChecked(false);
                }
                updatePageCompletion();
            }
        });

        setMessage(Messages.FetchJaegerTracesWizardPage_wizardDescriptionMessage);
        setControl(composite);
    }

    /**
     * Perform finish on this page. Import the selected traces
     *
     * @return True if the trace are imported
     */
    public boolean performFinish() {
        TmfTraceFolder tracesFolder = fTmfTraceFolder.getProject().getTracesFolder();
        if (tracesFolder != null) {
            List<String> checkedTraceIds = getCheckedTraces();
            IPath destinationFolderPath = fTmfTraceFolder.getPath().append(fTraceFolderName);
            String destinationSubPath = destinationFolderPath.makeRelativeTo(tracesFolder.getPath()).toOSString();
            SplitImportTracesOperation.splitAndImport(fJaegerJsonTrace, checkedTraceIds, destinationSubPath, tracesFolder);
            return true;
        }

        return false;
    }

    private List<String> getCheckedTraces() {
        List<String> traceIdList = new ArrayList<>();
        for (TableItem traceItem : fTracesTable.getItems()) {
            if (traceItem.getChecked()) {
                traceIdList.add(traceItem.getText(0));
            }
        }
        return traceIdList;
    }

    private void updatePageCompletion() {
        setErrorMessage(null);
        if (fJaegerJsonTrace == null || fJaegerJsonTrace.get(JAEGER_DATA_KEY).getAsJsonArray().size() == 0) {
            setPageComplete(false);
            return;
        }

        if (getCheckedTraces().isEmpty()) {
            setPageComplete(false);
            return;
        }

        if (fTmfTraceFolder.getResource().findMember(fTraceFolderName) != null) {
            setErrorMessage(Messages.FetchJaegerTracesWizardPage_errorFileName);
            setPageComplete(false);
            return;
        }

        setPageComplete(true);
    }
}
