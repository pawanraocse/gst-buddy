import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DocumentUploadComponent } from './document-upload.component';
import { MessageService } from 'primeng/api';
import { provideZonelessChangeDetection, NO_ERRORS_SCHEMA } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';

describe('DocumentUploadComponent', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;
  let messageServiceSpy: jasmine.SpyObj<MessageService>;

  beforeEach(async () => {
    const msgSpy = jasmine.createSpyObj('MessageService', ['add', 'messageObserver']);
    msgSpy.messageObserver = { subscribe: () => ({}) } as any;

    await TestBed.configureTestingModule({
      imports: [DocumentUploadComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideAnimations(),
        { provide: MessageService, useValue: msgSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    messageServiceSpy = TestBed.inject(MessageService) as jasmine.SpyObj<MessageService>;
    fixture = TestBed.createComponent(DocumentUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit valid xlsx files via filesSelected output', () => {
    const emitted: File[][] = [];
    const outputHarness = component.filesSelected;
    (outputHarness as any)['__emitterFn'] = (files: File[]) => emitted.push(files);

    // Use a direct spy subscription approach
    let emittedFiles: File[] | null = null;
    fixture.componentRef.setInput('disabled', false);

    const xlsxFile = new File(['data'], 'ledger.xlsx', { type: 'application/vnd.ms-excel' });
    const fileList = { 0: xlsxFile, length: 1, item: (i: number) => [xlsxFile][i] } as unknown as FileList;

    const event = { target: { files: fileList, value: '' } } as unknown as Event;

    spyOn(component.filesSelected, 'emit');
    component.onFileChange(event);

    expect(component.filesSelected.emit).toHaveBeenCalledWith([xlsxFile]);
  });

  it('should show error toast for invalid file type (.pdf)', () => {
    const pdfFile = new File(['data'], 'invoice.pdf', { type: 'application/pdf' });
    const fileList = { 0: pdfFile, length: 1, item: (i: number) => [pdfFile][i] } as unknown as FileList;
    const event = { target: { files: fileList, value: '' } } as unknown as Event;

    spyOn(component.filesSelected, 'emit');
    component.onFileChange(event);

    expect(messageServiceSpy.add).toHaveBeenCalledWith(jasmine.objectContaining({
      severity: 'error',
      summary: 'Invalid File Format'
    }));
    // Valid files count = 0, so filesSelected should NOT be emitted
    expect(component.filesSelected.emit).not.toHaveBeenCalled();
  });

  it('should not emit for invalid .csv files', () => {
    const csvFile = new File(['a,b'], 'data.csv', { type: 'text/csv' });
    const fileList = { 0: csvFile, length: 1, item: (i: number) => [csvFile][i] } as unknown as FileList;
    const event = { target: { files: fileList, value: '' } } as unknown as Event;

    spyOn(component.filesSelected, 'emit');
    component.onFileChange(event);

    expect(component.filesSelected.emit).not.toHaveBeenCalled();
    expect(messageServiceSpy.add).toHaveBeenCalledTimes(1);
  });

  it('should accept .xls files (old Excel format)', () => {
    const xlsFile = new File(['data'], 'ledger.xls', { type: 'application/vnd.ms-excel' });
    const fileList = { 0: xlsFile, length: 1, item: (i: number) => [xlsFile][i] } as unknown as FileList;
    const event = { target: { files: fileList, value: '' } } as unknown as Event;

    spyOn(component.filesSelected, 'emit');
    component.onFileChange(event);

    expect(component.filesSelected.emit).toHaveBeenCalledWith([xlsFile]);
    expect(messageServiceSpy.add).not.toHaveBeenCalled();
  });

  it('should not process files when disabled', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const xlsxFile = new File(['data'], 'ledger.xlsx', { type: 'application/vnd.ms-excel' });
    const dataTransfer = { files: { 0: xlsxFile, length: 1, item: (i: number) => [xlsxFile][i] } };
    const dragEvent = {
      preventDefault: jasmine.createSpy('preventDefault'),
      stopPropagation: jasmine.createSpy('stopPropagation'),
      dataTransfer
    } as unknown as DragEvent;

    spyOn(component.filesSelected, 'emit');
    component.onDrop(dragEvent);

    expect(component.filesSelected.emit).not.toHaveBeenCalled();
  });
});
