#!/usr/bin/perl
#Rob Wynne, Black Canyon Consulting
#Convert DataHasValue classes back
#to ObjectSomeValuesFrom with number Classes as objects

use strict;

my $i;
my $j;
my %fileMap = ();
my @numbers = ();
my %dataToObjectPropertyMap = (
	1142140007	=> "766953001",	#Count of active ingredient
	1142141006	=> "766954007",	#Count of base and modification pair	
	1142139005	=> "766952006",	#Count of base of active ingredient
	1142143009	=> "784276002", #Count of clinical drug type
	1142137007	=> "733723002",	#Has concentration strength denominator value
	1142138002	=> "733724008",	#Has concentration strength numerator value
	1142142004	=> "774161007", #Has pack size
	1142136003	=> "732946004",	#Has presentation strength denominator value
	1142135004	=> "732944001",	#Has presentation strength numerator value
	1148793005	=> "320081000221109" #Unit of presentation size quantity
);

my %objectToName = (
	766953001	=>	"Count of active ingredient",
	766954007	=>	"Count of base and modification pair",
	766952006	=>	"Count of base of active ingredient",
	784276002	=>	"Count of clinical drug type",
	733723002	=>	"Has concentration strength denominator value",
	733724008	=>	"Has concentration strength numerator value",
	774161007	=>	"Has pack size",
	732946004	=>	"Has presentation strength denominator value",
	732944001	=>	"Has presentation strength numerator value",
	320081000221109	=> "Unit of presentation size quantity"
);

my $objPropParent = "762705008";
my $numberParent = "260299005";

open(my $owlFile, $ARGV[0] ) or die "Couldn't open the owl file.\n";
my @owlLines = <$owlFile>;
my $out;

open my $finalOut, '>', $ARGV[1] or die "Couldn't create the output file.\n";
open my $debug, '>', "debug.txt" or die "Debug file couldn't be created.\n";

close $owlFile;

print "Input owl read into memory.\n";
print "Collecting numbers to add as classes.\n";

#Get all the numbers we need to add as classes
print $debug "Getting values in first pass.\n";
for($i=0; $i < @owlLines; $i++) {
	my $line = $owlLines[$i];
	if($line =~ /EquivalentClasses.*/) {
		while($line =~ /DataHasValue\(:\d+\s+"([^"]+)"/g) {
			my $value = $1;
			#print "==>\t$value\n";
			push(@numbers, $value);
		}
	}
}


#Sort and unique
my @sortedNumbers = sort { $a <=> $b } @numbers;
my @uniqNumbers = uniq( @sortedNumbers );

print $debug "Sorted numbers.\n---------\n";
for(@sortedNumbers) {
	print $debug $_."\n";
}

my $declarationsAdded = "false";
my $objectPropsAdded = "false";
my $numberClassesAdded = "false";

#Create a number map of values to identifiers
my %numberMap = ();
$i=0;
print $debug "Unique numbers\n--------\n";
foreach(@uniqNumbers) {
	++$i;
	print $debug $_."\t"."Number$i\n";
	$numberMap{$_} = "Number"."$i";
}

my $maxId = $i;

print "Split and serialize input for memory efficiency.\n";
$j=0;
for($i=0; $i < @owlLines; $i++) {
 if($i % 100000 == 0) {
   if(defined($out)) {close $out;}
   open $out, '>', "$j-split.owl";
   $fileMap{$j} = "$j-split.owl";
   $j++;
 }
 print $out $owlLines[$i];
}
@owlLines = ();

close $out;
my @files = glob("*-split.owl");

print "Making replacements (this could take a couple minutes).\n";
my $replacementsMade = 0;
for my $key (sort { $a <=> $b } keys %fileMap) {
	my $filename = $_;
	open(my $splitFile, $fileMap{$key}) or die "Couldn't open the split file.";
	my @lines = <$splitFile>;
	close $splitFile;
	for($i=0; $declarationsAdded eq "false" && $i < @lines; $i++) {
		if($lines[$i] =~ /Declaration\(Class\(/ ) {
			$lines[$i] = "Declaration(Class(:260299005))\n".$lines[$i];
			for($j=1; $j < $maxId; $j++) {
				$lines[$i] = "Declaration(Class(:Number$j))\n".$lines[$i];
			}
			for my $key (sort keys %dataToObjectPropertyMap) {
				$lines[$i] = "Declaration(ObjectProperty(:$dataToObjectPropertyMap{$key}))\n".$lines[$i];
			}
			$declarationsAdded = "true";
			print "Class declarations added.\n";
		}
	}
	for($i=0; $objectPropsAdded eq "false" && $i < @lines; $i++) {
		if($lines[$i] =~ /^# Object Property:/) {
			for my $key (sort keys %objectToName) {
				$lines[$i] = "# Object Property: <http://snomed.info/id/$key> ($objectToName{$key})\n\nAnnotationAssertion(rdfs:label :$key \"$objectToName{$key}\"\@en)\nSubObjectPropertyOf(:$key :$objPropParent)\n\n".$lines[$i];
			}
			$objectPropsAdded = "true";
			print "Object property classes added.\n";
		}
	}
	for($i=0; $numberClassesAdded eq "false" && $i < @lines; $i++) {
		if($lines[$i] =~ /^# Class:/) {
			$lines[$i] = "# Class: <http://snomed.info/id/260299005> (Number (qualifier value))\n\nAnnotationAssertion(rdfs:label :260299005 \"Number (qualifier value)\"\@en)\nAnnotationAssertion(skos:altLabel :260299005 \"Numbers\"\@en)\nAnnotationAssertion(skos:prefLabel :260299005 \"Number\"\@en)\nSubClassOf(:260299005 :362981000)\n\n".$lines[$i];
			for my $key (sort keys %numberMap) {
				$lines[$i] = "# Class: <http://snomed.info/id/$numberMap{$key}> ($key)\n\nAnnotationAssertion(rdfs:label :$numberMap{$key} \"$key\"\@en)\nSubClassOf(:$numberMap{$key} :$numberParent)\n\n".$lines[$i];
			}
			$numberClassesAdded = "true";
			print "Number classes added.\n";
		}
	}	
	for($i=0; $declarationsAdded eq "true" && $i < @lines; $i++) {
		if($lines[$i] =~ /EquivalentClasses.*(integer|decimal).*/) {
			#replace owl contstruct
			$lines[$i] =~ s/DataHasValue/ObjectSomeValuesFrom/g;
			
			#replace predicates
			for my $key (sort keys %dataToObjectPropertyMap) {
				$lines[$i] =~ s/:$key /:$dataToObjectPropertyMap{$key} /g;
			}
			
			#replace objects with number classes
			for my $key (sort keys %numberMap) {
				
				my $replace = $key;
				
				#I can't remember how to do this on-the-fly with substitution.
				if($replace =~ /(\d+)\.(\d+)/ ) {
					$replace = "$1\\.$2";
				}
				
				$lines[$i] =~ s/"$replace"\^\^xsd:(integer|decimal)/:$numberMap{$key}/g;
			}
			++$replacementsMade;
			if($replacementsMade % 1000 == 0) {
				print $replacementsMade." equivalent classes converted...\n";
			}
		}	
	}
	foreach(@lines) {
	   print $finalOut $_;
	}
 }
 
 print $replacementsMade." total equivalent classes converted.\n";
 print "Cleaning up intermediate files.\n";
 print "Data conversion complete.\n";

 close $debug;
 close $finalOut;
 
 for my $key (sort { $a <=> $b } keys %fileMap) {
	unlink $fileMap{$key};
 }

sub uniq {
  my %seen;
  return grep { !$seen{$_}++ } @_;
}
