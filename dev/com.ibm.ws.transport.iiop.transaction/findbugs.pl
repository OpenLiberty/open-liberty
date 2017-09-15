#!/usr/bin/perl -w
#*******************************************************************************
# Copyright (c) 2017 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************

my %bugs;
my $bug;

while (<>) {
  if (/<BugInstance type="(.*?)"/) {
    $bug = $1;
  } elsif (/<Class classname="(.*?)"/) {
    my $class = $1;
    ${bugs}{$bug}{$class} = 1;
  }
}

print <<EOF;
<FindBugsFilter>
  <!-- See http://findbugs.sourceforge.net/manual/filter.html for details of the syntax of this file -->
EOF

for my $bug (sort keys %bugs) {
  print "\n";
  print "  <Match>\n";
  my @classes = sort keys %{$bugs{$bug}};
  if (@classes == 1) {
      print "    <Class name=\"$classes[0]\"/>\n";
  } else {
    print "    <Or>\n";
    for my $class (@classes) {
      print "      <Class name=\"$class\"/>\n";
    }
    print "    </Or>\n";
  }
  print "    <Bug pattern=\"$bug\"/>\n";
  print "  </Match>\n";
}

print "</FindBugsFilter>\n";
