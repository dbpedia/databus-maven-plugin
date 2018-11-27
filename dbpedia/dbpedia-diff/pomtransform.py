#! /bin/python3


# Script for transforming the dbpedia maven databus pom to diff format


import xml.dom.minidom as minidom
import sys
import os 
import argparse


#Syntax: ./pomtransporm.py sourcedir targetdir
#For Version Change: ./pomtransform.py -v version sourcedir
#eg: ./pomtransform -v=2018.11.01 sourcedir

def handlePom(pomdom, pomtype):

    taglist = {"child":{"artifactId":"dbpedia-diff", "groupId":"org.dbpedia.databus.dbpedia-diff"}, "parent":{}}

    for tag in taglist[pomtype]:
        nodeList = pomdom.getElementsByTagName(tag)
        for node in nodeList:
            if node.nodeType == node.ELEMENT_NODE:
                textValue = node.childNodes[0].nodeValue
                if textValue != "databus-maven-plugin"  and textValue != "org.dbpedia.databus":
                    if textValue == "generic-spark-diff":
                        node.childNodes[0].nodeValue = taglist[pomtype][tag] 
                    elif textValue == "org.dbpedia.databus":
                        node.childNodes[0].nodeValue = taglist[pomtype][tag]
                    elif textValue == "mappings-diff":
                        node.childNodes[0].nodeValue = taglist[pomtype][tag]
                    elif textValue == "org.dbpedia.databus.mappings":
                        node.childNodes[0].nodeValue = taglist[pomtype][tag]
    return pomdom.toprettyxml()


def handleVersionChange(pomdom, version):
    for node in pomdom.getElementsByTagName("version"):
        if node.nodeType == node.ELEMENT_NODE:
            node.childNodes[0].nodeValue = version

    return pomdom.toprettyxml()


parser = argparse.ArgumentParser()
parser.add_argument("sourcedir", help="The source directory. Here should be your parent pom.")
parser.add_argument("--v", "--version", help="The version the poms should change to.")
parser.add_argument("--t", "--target", help="If you add a target directory the poms will be copied there, if not the poms in the source will change themself.")
args = parser.parse_args()

if args.t:
    targetdir=args.t
    open_var= "a"
else:
    open_var= "w"
    targetdir=args.sourcedir

sourcedir = args.sourcedir

 
if os.path.isfile(sourcedir+"/pom.xml"):

    parsed_dom = minidom.parse(sourcedir+"/pom.xml")
    with open(targetdir+"/pom.xml", open_var) as parentpom:
        if args.v: 
            print(handleVersionChange(parsed_dom, args.v), file=parentpom)
        else:
            print(handlePom(parsed_dom, "parent"), file=parentpom)
for thing in os.listdir(args.sourcedir):
    print(sourcedir+"/"+thing)
    if os.path.isdir(args.sourcedir+"/"+thing):
        if not os.path.isdir(targetdir+"/"+thing) and args.t:
            childpomDir = targetdir+"/"+thing+"-diff"
            os.mkdir(childpomDir)
        else:
            childpomDir= sourcedir+"/"+thing
        
        if os.path.isfile(sourcedir+"/"+thing+"/pom.xml"):
            parsed_dom = minidom.parse(sourcedir+"/"+thing+"/pom.xml")
            with open(childpomDir+"/pom.xml", open_var) as childpom:
                if args.v:
                    print(handleVersionChange(parsed_dom, args.v), file=childpom)
                else:
                    print(handlePom(parsed_dom, "child"), file=childpom)
