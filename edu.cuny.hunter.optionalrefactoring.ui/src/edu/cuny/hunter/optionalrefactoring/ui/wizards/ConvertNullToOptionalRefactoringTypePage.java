package edu.cuny.hunter.optionalrefactoring.ui.wizards;

import java.util.function.BiConsumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import edu.cuny.hunter.optionalrefactoring.core.messages.Messages;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.ConvertNullToOptionalRefactoringProcessor;
import edu.cuny.hunter.optionalrefactoring.core.refactorings.RefactoringSettings.Choice;

public class ConvertNullToOptionalRefactoringTypePage extends UserInputWizardPage implements IWizardPage {

	private static final String DESCRIPTION = Messages.ConvertNullToOptionalTypePage_Description;

	private static final String DIALOG_SETTING_SECTION = "ConvertN2O"; //$NON-NLS-1$

	public final String PAGE_NAME;

	private ConvertNullToOptionalRefactoringProcessor processor;
	private IDialogSettings settings;

	public ConvertNullToOptionalRefactoringTypePage(String typePageName) {
		super(typePageName);
		this.PAGE_NAME = typePageName;
		this.setDescription(DESCRIPTION);
	}

	private void addBooleanButton(String text, Choice choice, BiConsumer<Boolean, Choice> valueConsumer,
			Composite result) {
		Button button = new Button(result, SWT.CHECK);
		button.setText(text);
		boolean value = this.processor.settings().get(choice);
		valueConsumer.accept(value, choice);
		button.setSelection(value);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Button btn = (Button) event.getSource();
				boolean selection = btn.getSelection();
				ConvertNullToOptionalRefactoringTypePage.this.settings.put(text, selection);
				valueConsumer.accept(selection, choice);
			}
		});
	}

	@Override
	public boolean canFlipToNextPage() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createControl(Composite parent) {
		ProcessorBasedRefactoring processorBasedRefactoring = (ProcessorBasedRefactoring) this.getRefactoring();
		org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor refactoringProcessor = processorBasedRefactoring
				.getProcessor();
		this.setProcessor((ConvertNullToOptionalRefactoringProcessor) refactoringProcessor);
		this.loadSettings();

		Composite result = new Composite(parent, SWT.NONE);
		this.setControl(result);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		result.setLayout(layout);

		// set up buttons.
		for (Choice choice : Choice.values())
			this.addBooleanButton("Refactor Fields", choice, this.processor.settings()::set, result);

		this.updateStatus();
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this.getControl(),
				"optimize_logging_level_wizard_page_context");
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Control getControl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Image getImage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IWizardPage getNextPage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IWizardPage getPreviousPage() {
		// TODO Auto-generated method stub
		return null;
	}

	private ConvertNullToOptionalRefactoringProcessor getProcessor() {
		return this.processor;
	}

	@Override
	public String getTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IWizard getWizard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isPageComplete() {
		// TODO Auto-generated method stub
		return false;
	}

	private void loadSettings() {
		this.settings = this.getDialogSettings().getSection(DIALOG_SETTING_SECTION);
		if (this.settings == null) {
			this.settings = this.getDialogSettings().addNewSection(DIALOG_SETTING_SECTION);
			for (Choice choice : Choice.values())
				this.settings.put(choice.toString(), this.getProcessor().settings().get(choice));
		}
	}

	@Override
	public void performHelp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setImageDescriptor(ImageDescriptor image) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPreviousPage(IWizardPage page) {
		// TODO Auto-generated method stub

	}

	private void setProcessor(ConvertNullToOptionalRefactoringProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void setTitle(String title) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWizard(IWizard newWizard) {
		// TODO Auto-generated method stub

	}

	private void updateStatus() {
		this.setPageComplete(true);
	}

}
