#! /bin/python3


# Script for transforming the dbpedia maven databus pom to diff format


import xml.dom.minidom as minidom
import sys
import os 



#Syntax: ./pomtransporm.py sourcedir targetdir
#For Version Change: ./pomtransform.py -v version sourcedir
#eg: ./pomtransform -v 2018.11.01 sourcedir

def handlePom(pomdom, pomtype):

    taglist = {"child":{"artifactId":"%s%-diff", "label":"-diff", "datasetDescription":"The diffs to %s%"}, "parent":{"module":"%s%-diff", "artifactId":"%s%-diff", }}

    for tag in taglist[pomtype]:
        nodeList = pomdom.getElementsByTagName(tag)
        for node in nodeList:   
                if node.nodeType == node.ELEMENT_NODE:
                    textValue = node.childNodes[0].nodeValue
                    if textValue != "databus-maven-plugin"  and textValue != "org.dbpedia.databus":
                        if tag == "label":
                            node.childNodes[0].nodeValue = textValue.split("@")[0] + taglist[pomtype][tag] +"@"+ textValue.split("@")[1]
                        else:
                            node.childNodes[0].nodeValue = taglist[pomtype][tag].replace("%s%", textValue)
    return pomdom.toprettyxml()


def handleVersionChange(pomdom, version):
    for node in pomdom.getElementsByTagName("version"):
        if node.nodeType == node.ELEMENT_NODE:
            node.childNodes[0].nodeValue = version

    return pomdom.toprettyxml()

if sys.argv[1] == "-v" or sys.argv[1] == "-version"
    sourcedir = sys.argv[3]
    version = sys.argv[2]
else:
    sourcedir = sys.argv[1]

targetdir = sys.argv[2]

parentpomDir = targetdir+"/"+sourcedir.split("/")[-1]
if not os.path.isdir(parentpomDir):
    os.mkdir(parentpomDir)
    
for thing in os.listdir(sourcedir):
    if os.path.isfile(sourcedir+"/"+thing):
        with open(parentpomDir+"/pom.xml", "a") as parentpom:
            print(handlePom(minidom.parse(sourcedir+"/"+thing), "parent"), file=parentpom)
    elif os.path.isdir(sourcedir+"/"+thing):
        childpomDir = targetdir+"/"+sourcedir.split("/")[-1]+"/"+thing+"-diff"
        os.mkdir(childpomDir)
        if os.path.isfile(sourcedir+"/"+thing+"/pom.xml"):
            with open(childpomDir+"/pom.xml", "a") as childpom:
                print(handlePom(minidom.parse(sourcedir+"/"+thing+"/pom.xml"), "child"), file=childpom)
    else:
        print(" is no file or directory")
