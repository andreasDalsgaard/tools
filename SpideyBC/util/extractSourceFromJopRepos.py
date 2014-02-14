#!/bin/python

import os, sys
import shutil
import argparse

def main(root, target, mapping):
    SRCTARGET=target
    JOPROOT=root
    CLASSDIR=JOPROOT+"java/target/dist/classes/"
    SRCDIR=JOPROOT+"java/target/src/"

    for o in os.listdir(CLASSDIR):
        if not mapping.has_key(o):
            print('Mapping: '+o+' unknown')
            sys.exit(1)
        else:
            #Go through files in class directory and create file list (may contain "files" that does not exist due to private classes)
            file_filter = {}
            for path, subdirs, files in os.walk(CLASSDIR+o):
                for filename in files:
                    mappedFile = mapping[o]+path[len(CLASSDIR+o):]+'/'+filename.split('.')[0]
                    file_filter[mappedFile]=1
                    if o == "java":
                        try:
                            mappedFile = mapping["ALT"+o]+path[len(CLASSDIR+o):]+'/'+filename.split('.')[0]
                            file_filter[mappedFile]=1
                        except:
                            pass

    
            #Go through files in mapped dir and copy files to target dir if file in filter
            for path, subdirs, files in os.walk(SRCDIR+mapping[o]):
                for filename in files:
                    relativePath = path[len(SRCDIR):]+'/'+filename.split('.')[0]
                    if file_filter.has_key(relativePath):
                        dst_file = SRCTARGET+o+path[len(SRCDIR+mapping[o]):]+'/'+filename
                        dstdir = os.path.dirname(dst_file)
                        try:
                            os.makedirs(dstdir) 
                        except OSError:
                            #print dstdir
                            pass
                        shutil.copy(path+'/'+filename, dst_file)


            if o == "java":
                try:
                    for path, subdirs, files in os.walk(SRCDIR+mapping["ALT"+o]):
                        for filename in files:
                            relativePath = path[len(SRCDIR):]+'/'+filename.split('.')[0]
                            if file_filter.has_key(relativePath):
                                dst_file = SRCTARGET+o+path[len(SRCDIR+mapping["ALT"+o]):]+'/'+filename
                                dstdir = os.path.dirname(dst_file)
                                try:
                                    os.makedirs(dstdir) 
                                except OSError:
                                    #print dstdir
                                    pass
                                shutil.copy(path+'/'+filename, dst_file)
                except:
                    pass


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Copy source files for examples compiled using JOP makefile.')
    parser.add_argument('--joproot', help='The root directory of your JOP repository.', required=True)
    parser.add_argument('--srctarget', help='Target directory for where to copy the source files to.', required=True)
    parser.add_argument('--mapping', metavar='map', type=str, nargs='+', help='List of string mappings "classdir:sourcedir ..." used for determining which java source files the example was compiled from. E.g., com:common/com java:jdk_base/java joprt:common/joprt util:common/util ', required=True)
    
    args = parser.parse_args()
    mappings = {}
    for mapping in args.mapping:
        (key, value) = mapping.split(':')
        mappings[key] = value
   
    if args.joproot[-1] != '/':
        args.joproot = args.joproot+'/'
        
    if args.srctarget[-1] != '/':
        args.srctarget = args.srctarget+'/'

    main(args.joproot, args.srctarget, mappings)
