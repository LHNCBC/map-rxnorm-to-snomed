# RXNORM in OWL

## Goal

Generate various flavors of RXNORM in OWL with data from the RxNav REST API, including a QA report detailing clinical drug misalignments between RXNORM and SNOMED CT International.

### Pre-requisites

- An Internet connection open to
  - the RxNav REST API (https://rxnav.nlm.nih.gov/REST/version should return something)
  - GitHub and the snomed-owl-toolkit project (https://github.com/IHTSDO/snomed-owl-toolkit)
  - an RF2 distribution of SNOMED International from NLM (https://www.nlm.nih.gov/healthit/snomedct/international.html)
  - sonatype.org
- Sufficient memory to run the program (15GB is optimal)
- Enough diskspace to store all outputs (at least 5G)
- Java 17
- Maven
- Perl (tested with 5.16.3)

### Prepare and Build

- Download the most recent snomed-owl-toolkit*executable.jar (https://github.com/IHTSDO/snomed-owl-toolkit/releases)
- Download the monthly RF2 distribution of SNOMED International from NLM
- Generate the SNOMED CT OWL file from RF2 with the command found here. (https://github.com/IHTSDO/snomed-owl-toolkit/#snomed-rf2-to-owl-file-conversion)
- Download and extract this repository OR:

```
$ git clone https://this.url/map-rxnorm-to-snomed.git
$ cd map-rxnorm-to-snomed
$ mvn clean package
```

### Deploy

```
$ cd preprocessing
$ perl convert-from-datahasvalue.pl /absolute/uri/to/snomed-ct-in.owl /absolute/uri/to/output-numbersAsClasses.owl
$ cd ..
$ cp target/MapRxNormToSnomed-0.4.0-SNAPSHOT-jar-with-dependencies.jar .
$ java -jar -Xmx15000m MapRxNormToSnomed-0.4.0-SNAPSHOT-jar-with-dependencies.jar /absolute/uri/to/snomed-ct-in.owl /absolute/uri/to/output-numbersAsClasses.owl
```

Many messages will display to standard out including Reasoner INFO logs that cannot be suppressed.
All may be safely ignored.

The process is complete after the following is output:
```
...
********** Saving Defined OWL **********

Saving file as ./Defined-RxNorm-with-SNCT-classes-2023-11-28.owl
Nov 28, 2023 5:20:52 PM org.semanticweb.elk.reasoner.Reasoner shutdown
INFO: ELK reasoner has shut down

Finished mapping in xxxx seconds.
```

### Outputs

_These are experimental datasets in support of the RXNORM in OWL project._
_These outputs are not reviewed by pharmacists and NLM makes no claim of completeness or accuracy._


- Pilot-Defined-RxNorm-with-SNCT-classes_YYYY-MM-DD.owl 
  - A Defined RxNorm version without the SNOMED source. Classes are defined with SNOMED content when available. Contains Active SCDs only, has concrete domains. Integers and decimals are represented in the form of concrete domains (e.g., "1"^^xsd:integer and ".5"^^xsd:decimal).
  - The (experimental) U.S. National Extension
- Defined-RxNorm-with-SNCT-classes_YYYY-MM-DD.owl
  - A Defined RxNorm version without the SNOMED source. Classes are defined with SNOMED content when available. Contains all SCDs and SBDs regardless of status. Integers and decimals are represented in the form of concrete domains (e.g., "1"^^xsd:integer and ".5"^^xsd:decimal).
- RxNorm2Snomed_YYYY-MM-DD_HH-MM-SS.owl
  - NLM version. All Active SCDs, all classes in one namespace, numbers represented as classes to support ELK reasoning and the Excel Monster QA report


### Opening RXNORM in OWL with Protege

Follow instructions found in documentation/RXNORM-in-OWL.docx

### Custom annotations

- asserted equivalence
  - The RXCUI is mapped to a SNOMED CT ID in RXNORM, represented as a boolean
- inferred equivalnce
  - The SNOMED CT concept is inferred by the ELK reasoner to a RXNORM logical definition, represented as true

These may be added to the Defined files with the post-processing script run as:

```
$ cd postprocessing
$ perl add-custom-annotations-to-defined.pl ../RxNorm2Snomed_YYYY-MM-DD_HH-MM-SS.owl ../Pilot-Defined-RxNorm-with-SNCT-classes-YYYY-HH-MM.owl
Discovered 29698 Asserted annotations.
Discovered 4951 Inferred annotations.
Finished adding annotations to ../Pilot-Defined-RxNorm-with-SNCT-classes-YYYY-HH-MM-with-custom-annotations.owl in 5 seconds. 
```

