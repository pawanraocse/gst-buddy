## **1 Objective**

This document encapsulates the backend design of the module calculating the late fees payable for GSTR1 filing in accordance with the CGST Act. Check the [PRD](https://docs.google.com/document/d/17_Uk73mAMABYu7xrQ-E63hO0qOvMVGG11OqZtU88KNw/edit?tab=t.0) for detailed requirements.

## **2 Pre-requisites**

- Relief Window details: Static data stored at our backend. [Sample](https://docs.google.com/spreadsheets/d/1TBfYaIBaaknZpiTPwr8Tb6Tn7uOGGnb9t4E6-odfELQ/edit?gid=653012028#gid=653012028)  
- GSTR1 form: PDF uploaded by the user. [Sample GSTR1](https://drive.google.com/open?id=1_UcvfluSOQr4RRhVq6in4nKDbw_aYFci)

## **3 Modules needed:**

1. ### **GSTR1 Relief Window details store:**

   This is the static master data for the various reliefs applicable to the late fees. We will save it in the database table   
   **GSTR1\_Late\_Fee\_Relief\_Windows**  
   **Schema:**  
- relief\_id (pk)		varchar  
- notification\_no	varchar  
- relief\_type		varchar  
- tax\_period\_from	DATE  
- tax\_period\_to	DATE  
- filing\_date\_from	DATE  
- filing\_date\_to 	DATE  
- return\_type		VARCHAR  
- max\_late\_fee	INTEGER (convert it to lowest currency denomination and save in case we need to support decimals in future?)  
- priority		INTEGER  
- legal\_notes		VARCHAR  
- created\_on		TIMESTAMP  
- updated\_on		TIMESTAMP  
    
  This table will be populated by a script which will insert [this data](https://docs.google.com/spreadsheets/d/1TBfYaIBaaknZpiTPwr8Tb6Tn7uOGGnb9t4E6-odfELQ/edit?gid=653012028#gid=653012028) into the table. We also need to have an update script ready for future changes.  
    
  Corresponding JAVA class:  
  record GSTR1LateFeeReliefWindows (  
   String reliefId,  
   String notificationNo,  
   String reliefType,  
   Date taxPeriodFrom,  
   Date taxPeriodTo,  
   Date filingDateFrom,  
   Date filingDateTo,  
   ReturnType returnType, //NIL, NON\_NILL enum  
   int maxLateFee,  
   int priority,  
   String legalNotes,  
   OffsetDateTime createdOn,  
   OffsetDateTime updatedOn  
  ) {}  
  


2. ### **GSTR1LateFeeCalculator module(P0):**

   This module will accept the GSTR1 returns pdf contents, break it down to simpler tasks and call the relevant subtasks and return the final output to the user.  
   class GSTR1LateFeeCalculator {  
    GSTR1FormInput gstr1FormInput;  
     
    public GSTR1LateFeeCalculator(GSTR1FormInput gstr1FormInput) {  
      this.gstr1FormInput \= gstr1FormInput;  
    }  
     
    GSTR1LateFeeResponse run() {  
      GSTR1ReturnPeriodDetails gstr1ReturnPeriodDetails \= new GSTR1ReturnContentsParser(gstr1FormInput).run();  
      GSTR1LateFeeResponse gstr1LateFeeResponse \= new GSTR1MaxLateFeeCalculator(gstr1ReturnPeriodDetails).run();  
      return gstr1LateFeeResponse;  
    }  
   }  
     
   

3. ### **GSTR1ReturnContentsParser module(P0):**

   This module will parse the pdf contents for the GSTR1 return form and return an object understandable by the code. **Assumption** is that the pdf has already been parsed and the contents extracted before GSTR1LateFeeCalculator has been called. The parser will most likely be called outside the GSTR1LateFeeCalculator and all the required fields will already be extracted. Will change it once we go through all the rules and add the figure out which fields need to be extracted.  
     
   class GSTR1ReturnContentsParser {  
     
    public GSTR1ReturnContentsParser(GSTR1FormInput gstr1FormInput) {  
    }  
     
    public GSTR1ReturnPeriodDetails run() {  
      //parse the PDF contents and return the details  
      return new GSTR1ReturnPeriodDetails();  
    }  
   }  
     
   

   

   #### Input: 

   	  
   class GSTR1FormInput {  
     
    private String gstr1FormContents;  
    private final int schemaVersion \= 1;  
   }

   

   #### Output:

     
   class GSTR1ReturnPeriodDetails{  
    Month taxPeriod;  
    String fiscalYearStart; //yyyy  
    String fiscalYearEnd; //yyyy  
    String gstIn;  
    String arn;  
    Date arnDate;  
    ReturnType returnType; //NIL, NON\_NILL, ALL enum  
    final int schemaVersion \= 1;  
   }

   

   We will use a common PDFReader that will read the PDF and return the contents. GSTR1PDFParser will parse the content to return the GSTR1ReturnPeriodDetails.  
     
   \*\*We will extract all other details as well. But right now focussing only on the fields needed for late fee calculation.This section is incomplete as we still need to write the details of how we will parse the extracted PDF contents but before that we have to decide how to extract the PDF contents.  
     
   **~~\[ How to extract the PDF contents? (out of scope for this feature as we should have already extracted it but keeping it here as this is the most important step)~~**  
   	~~Explore the following options:~~  
- ~~Apache PDFbox~~  
- [~~Adobe PDF Extract API~~](https://developer.adobe.com/document-services/docs/overview/pdf-extract-api/quickstarts/java/)  
- ~~Tesseract (for OCR) (P1??)~~  
- ~~iText~~  
- ~~Explore Python libraries as well in case they work better~~  
- ~~Any library will return a block of text upon reading the PDF. We have to check whether it is possible to treat the PDF as a form and read the form fields, or read in a structured format like JSON or map.\]~~  
  Using Python pdfplumber for the pdf parsing. We deploy the python parsers as a small HTTP microservice and call it from Java over HTTP. We can maintain some secret key between the Java and the Python services.  
    
  Java Backend  →  HTTP POST (multipart/json)  →  Python FastAPI service  →  JSON response

  (We can also explore other options for calling the Python service like JPype, Jython etc. But I do not have much knowledge about them, and http service seemed the simplest, but open to options)

4. ### **Max late fee calculator(P0):**

   This module will calculate the and return the max late fee.  
     
   class GSTR1MaxLateFeeCalculator {  
     
    public GSTR1MaxLateFeeCalculator(GSTR1ReturnPeriodDetails gstr1ReturnPeriodDetails) {  
    }  
     
    public GSTR1LateFeeResponse run() {  
      //calculate the late fees  
      return new GSTR1LateFeeResponse();  
    }  
   }  
     
   **Late fee calculation steps:**  
- Calculate delay days  
  - delay\_days \= max(arnDate \- 10th of taxPeriod+1, 0\)  
- If delay\_days \== 0, return late\_fee \= 0

  else

- normal\_late\_fee \= min(delay\_days × 200, 10000\)  
- Fetch the relief window from **GSTR1\_Late\_Fee\_Relief\_Windows** DB satisfying the conditions:  
  - tax\_period between (tax\_period\_from, tax\_period\_to)  
    AND  
    filing\_date between (filing\_date\_from, filing\_date\_to)  
    AND  
    returnType IN (gstr1ReturnPeriodDetails.returnType, ReturnType.ALL)  
    AND  
    min(priority)  
      
    This should return exactly 1 relief window.   
    max\_allowed\_late\_fee \= appliedReliefWindow.maxLateFee  
- final\_late\_fee \= min(normal\_late\_fee, allowed\_max)  
- GSTR1MaxLateFeeCalculator will finally return GSTR1LateFeeResponse  
    
  class GSTR1LateFeeResponse {  
   int lateFee;  
   int normalLateFee;  
   int waiverAmount;  
   GSTR1LateFeeReliefWindows appliedReliefWindow;  
   final int schemaVersion \= 1;  
  }

5. ### **Post calculation**

- Save the GSTR1 late fee details in the DB 

  **GSTR1\_Late\_Fees**

  **Schema:**

- job\_id(pk)	varchar  
- arn		varchar  
- user\_id	varchar(the logged in user)  
- profile\_id	varchar(profile for which they are running the details, we can use gstin as profile id)  
- file\_id		varchar(the file name for which we are running)  
- applied\_relief\_id foreign key references GSTR1\_Late\_Fee\_Relief\_Windows(relief\_id)  
- late\_fee	integer  
- waiver\_amt	integer


  We can keep this for tracking internally and clean it up periodically through a scheduled job.


- Cleanup the uploaded PDF(but this has to done after all the rules have been processed)

## **4 Exception Handling**

	In case of any exception, GSTR1LateFeeCalculator will throw a GSTR1LateFeeCalculationException which will be handled in the main calling class.  
class GSTR1LateFeeCalculationException extends Exception {  
 ErrorCode errorCode;  
 public GSTR1LateFeeCalculationException (ErrorCode errorCode, String message) {  
   super(message);  
   this.errorCode \= errorCode;  
 }

 public ErrorCode getErrorCode() {  
   return errorCode;  
 }  
}

## 

## **5 Questions/Assumptions**:

- We will also need a DB to track the processing status of a particular “job”(by which I mean the health check run started by the user after uploading all relevant files) and track if any check has failed. We will also need this in future for billing purposes to keep track of how many runs we have processed. Out of scope for this document, added for tracking.  
- Are there any legal implications of saving the details eg. GSTR1\_Late\_Fees in our DB?  
- Will it always be a true PDF or do we need to support scanned images as well? Maybe we can keep it as P1?  
- If the user uploads the same PDF again, we will treat it as a new file and process it again. We can reuse the entry from **GSTR1\_Late\_Fees** DB, in which case we also have to check if a new relief rule has been added. Also we have to parse the PDF anyway to get the arn. So we will not save much time.  
- How many files can be uploaded per run? Can there be multiple GSTR1 return forms in one run? Only after that can we decide how to save the data.  
- This document solely focuses on the calculation of GSTR1 late fee. Things will definitely evolve one we take all the rules into consideration as we will reuse certain sections.  
- What will happen if the run partially fails? Out of scope for this document, added for tracking for discussion.

  