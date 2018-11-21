#! /bin/python3


# Script for transforming the dbpedia maven databus pom to diff format


import xml.dom.minidom as minidom
import sys
import os 


def handlePom(pomdom, pomtype):

    taglist = {"child":{"artifactId":"-diff", "label":"-diff", "datasetDescription":"The diffs to "}, "parent":{"module":"-diff", "artifactId":"-diff", }}

    for tag in taglist[pomtype]:
        nodeList = pomdom.getElementsByTagName(tag)
        for node in nodeList:   
                if node.nodeType == node.ELEMENT_NODE:
                    textValue = node.childNodes[0].nodeValue
                    if textValue != "databus-maven-plugin"  and textValue != "org.dbpedia.databus":
                        if taglist[pomtype][tag] != "-diff":
                            node.childNodes[0].nodeValue = taglist[pomtype][tag] + textValue  
                        elif tag == "label":
                            node.childNodes[0].nodeValue = textValue.split("@")[0] + taglist[pomtype][tag] +"@"+ textValue.split("@")[1]
                        else:
                            node.childNodes[0].nodeValue = textValue + taglist[pomtype][tag]
    return pomdom.toprettyxml()




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
