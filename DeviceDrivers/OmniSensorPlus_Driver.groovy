/**
 *
 * Sensor Groups+_OmniSensor
 *
 * Copyright 2022 Ryan Elliott
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * v1.0		RLE		Creation
 * v1.1     RLE     Added list attribute to show triggered devices
 * v1.2     RLE     Added threshold attribute
 * v1.3     RLE     Combined attributes
 * v1.4     RLE     Added on/off for switches
 * v1.5     RLE     Added missing Lux capability; added power
 * v1.6     RLE     Added device listing for the power sensor
 */

metadata {
	definition(
    name: "Sensor Groups+_OmniSensor",
    namespace: "rle.sg+",
    author: "Ryan Elliott") 
		{
		capability "ContactSensor"
        capability "Carbon Monoxide Detector"
        capability "Motion Sensor"
        capability "Smoke Detector"
        capability "Water Sensor"
        capability "Relative Humidity Measurement"
        capability "PowerMeter"
        capability "IlluminanceMeasurement"
        capability "Switch"
            

        attribute "displayList", "string"
        //Contact attributes	
		attribute "contact", "enum", ["closed", "open"]
		attribute "TotalCount", "number"
		attribute "TotalOpen", "number"
		attribute "TotalClosed", "number"
		attribute "OpenList", "string"
		attribute "OpenThreshold", "number"
        //CO attributes
        attribute "carbonMonoxide", "enum", ["clear", "detected"]
        attribute "TotalDetected", "number"
        attribute "TotalClear", "number"
        attribute "CODetectedList", "string"
        attribute "CODetectedThreshold", "number"
        //Motion attributes
        attribute "motion", "enum", ["active","inactive"]
        attribute "TotalActive", "number"
        attribute "TotalInactive", "number"
        attribute "ActiveThreshold", "number"
        attribute "ActiveList", "string"
        //Smoke attributes
        attribute "smoke", "enum", ["clear", "detected"]
        attribute "SmokeDetectedList", "string"
        attribute "SmokeDetectedThreshold", "number"
        //Leak attributes
        attribute "water", "enum", ["dry", "wet"]
        attribute "TotalWet", "number"
        attribute "TotalDry", "number"
        attribute "WetList", "string"
        attribute "WetThreshold", "number"
        //Humidity attributes
        attribute "MaxHumidity", "number"
        attribute "AverageHumidity", "number"
        attribute "MinHumidity", "number"
        //Tempature attributes
        attribute "MaxTemp", "number"
        attribute "AverageTemp", "number"
        attribute "MinTemp", "number"
        //Lux attributes
        attribute "MaxLux", "number"
        attribute "AverageLux", "number"
        attribute "MinLux", "number"
        //Switch attributes
        attribute "TotalOn", "number"
        attribute "TotalOff", "number"
        attribute "OnList", "string"
        attribute "OnThreshold", "number"
        attribute "switch", "enum", ["on", "off"]
        //Power attributes
        attribute "TotalPower", "number"
        attribute "PowerDevices", "string"
        //CO attributes
        attribute "lock", "enum", ["unlocked", "locked"]
        attribute "TotalLocked", "number"
        attribute "TotalUnlocked", "number"
        attribute "LockedList", "string"
        attribute "LocksLockedThreshold", "number"
	}
}

def installed() {
	log.warn "installed..."
}

def updated() {
	log.info "updated..."
}

def on() {
    def descriptionText = "The on command for SG+_OmniSensors does nothing"
    log.warn "${descriptionText}"
    sendEvent(name: "TheButtonsDoNothing", value: "The on button does nothing", descriptionText: descriptionText)
}

def off() {
    def descriptionText = "The off command for SG+_OmniSensors does nothing"
    log.warn "${descriptionText}"
    sendEvent(name: "TheButtonsDoNothing", value: "The off button does nothing", descriptionText: descriptionText)
}