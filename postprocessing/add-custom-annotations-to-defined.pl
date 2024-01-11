#!/usr/bin/perl
#Rob Wynne, BCC
# post-processing step necessary to harvest custom equivalent annotations
# from the "with numbers as classes" RxNorm2Snomed*.owl file (ELK issue #61)
# and append them to the *Defined*.owl files in Machester Functional OWL Syntax

use strict;
my $start = time();

open( my $input, $ARGV[0]) or die "Couldn't open input!\n";

my @lines = <$input>;

my $i;
my $assertedCount = 0;
my $inferredCount = 0;
my @annotations = ();

#We want:
#AnnotationAssertion(:Asserted :Rx0 "false")
#AnnotationAssertion(:Inferred <http://snomed.info/id/1003494004> "true")
#...
#
#We don't want:
#AnnotationAssertion(:Asserted <http://snomed.info/id/###-FS> "true")  [numbers as classes]

#To:
#AnnotationAssertion(:Asserted <http://snomed.info/id/895696005> "true")
#AnnotationAssertion(:Inferred <http://snomed.info/id/895696005> "false")
#AnnotationAssertion(:Asserted <http://mor.nlm.nih.gov/RXNORM/0> "false")
#...
#)

for($i=0; $i < @lines; $i++) {
	my $line = $lines[$i];
	chomp($line);
	if($line =~ /^(AnnotationAssertion\(:(Asserted|Inferred)\s<?)((:Rx)|(http:\/\/snomed\.info\/id\/))(\d+)(>)?(\s\".*)$/ )	{
		my $annotationProperty = $1;
		my $namespace = $3;
		my $fragment = $6;
		my $tail = $4; #-FS preclusion
		my $filler = $8;

		#print $annotationProperty."\n";
		#print $namespace."\n";
		#print $fragment."\n";
		#print $filler."\n";
		
		if($annotationProperty =~ /.*Asserted.*/) {
			$assertedCount++;
		} else {
			$inferredCount++;
		}
		
		my $iri = "";
		if($namespace =~ /:Rx/) {
			$iri = "<http://mor.nlm.nih.gov/RXNORM/".$fragment.">";
		} else {
			$iri = $namespace.$fragment.">";
		}
		
		#my $iri = $namespace.$fragment.">";
		
		my $annotation = $annotationProperty.$iri.$filler."\n";
		#print $annotation;
		push(@annotations,$annotation);
	}
}

close $input;
@lines = ();

print "Discovered $assertedCount Asserted annotations.\n";
print "Discovered $inferredCount Inferred annotations.\n";

#Insert
open( my $defined, $ARGV[1]) or die "Couldn't open the Defined file!\n";
my $definedOutput = $ARGV[1];
$definedOutput =~ s/(.*)\.owl/$1\-with\-custom\-annotations\.owl/;
@lines = <$defined>;

my $nextToLastLineNumber = @lines - 2; #Manchester Syntax ends with )
foreach(@annotations) {
	$lines[$nextToLastLineNumber] .= $_;
}

close $defined;

#Serialize
open my $finalOut, '>', $definedOutput or die "Couldn't create the output file.\n";

foreach(@lines) {
	print $finalOut $_;
}

close $finalOut;
my $end = time();
my $total = $end - $start;
print "Finished adding annotations to ".$definedOutput." in ".$total." seconds.\n";
