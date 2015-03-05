#!/usr/bin/perl

$mydroid_dir = $ARGV[0];
$installed = "/out/target/product/generic/installed-files.txt";
$sysapp_dir = "/out/target/common/obj/APPS/";
$fw_dir = "/out/target/common/obj/JAVA_LIBRARIES/";

open FILE, "<$mydroid_dir$installed" or die $!;
while (<FILE>) {
	chomp($_);
	if ($_ =~ m/  \/system\/app\/(.*).apk/) {
		$sysapp = $1; # gets the system app directory path, ie "Calendar/Calendar"
		if (-e "$mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes-full-debug.jar") {
			system("cp $mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes-full-debug.jar $sysapp.jar");
		} elsif (-e "$mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes.jar") {
			system("cp $mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes.jar $sysapp.jar");
		} else {
			# newer Android builds removed the subdirectory, so trim the path
			@fw_parts = split m%/%, $fw; # splits on /
                	$fw = shift @fw_parts; # grabs first array element, ie "Calendar"
			if (-e "$mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes-full-debug.jar") {
				system("cp $mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes-full-debug.jar $sysapp.jar");
			} elsif (-e "$mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes.jar") {
				system("cp $mydroid_dir$sysapp_dir$sysapp"."_intermediates/classes.jar $sysapp.jar");
			} else {
				print "???Cannot find compiled classes for $sysapp???\n";
			}
		}
	} elsif ($_ =~ m/  \/system\/framework\/(.*).jar/) {
		$fw = $1;
		if (-e "$mydroid_dir$fw_dir$fw"."_intermediates/classes-full-debug.jar") {
			system("cp $mydroid_dir$fw_dir$fw"."_intermediates/classes-full-debug.jar $fw.jar");
		} elsif (-e "$mydroid_dir$fw_dir$fw"."_intermediates/classes.jar") {
			system("cp $mydroid_dir$fw_dir$fw"."_intermediates/classes.jar $fw.jar");
		} else {
			print "???Cannot find compiled classes for $fw???\n";
		}
	}
}
close FILE;

# NFC is not installed to generic target, thus not found in the installed-files.txt
if (-e "$mydroid_dir$sysapp_dir"."Nfc_intermediates/classes-full-debug.jar") {
	system("cp $mydroid_dir$sysapp_dir"."Nfc_intermediates/classes-full-debug.jar Nfc.jar");
}
