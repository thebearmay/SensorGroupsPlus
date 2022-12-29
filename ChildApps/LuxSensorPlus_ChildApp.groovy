/**
 *
 * Sensor Groups+_Lux
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
 */
 
definition(
    name: "Sensor Groups+_Lux",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of lux sensors.",
    category: "Convenience",
    parent: "rle.sg+:Sensor Groups+",
    iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "mainPage")
}

def mainPage() {
	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","<b>App Name</b>")) {
            label title: "<b>Enter a name for this child app.</b>"+
            "<br>This will create a virtual lux sensor which reports the high, low, and average lux based on the sensors you select.", required:true,width:6
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph "<b>Please choose which sensors to include in this group.</b>"
			input "luxSensors", "capability.illuminanceMeasurement", title: "Lux sensors to monitor", multiple:true, required:true,width:6
        }
        
        section(getFormat("header","<b>Options</b>")) { 
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
        }
    }
}

def installed() {
	initialize()
}

def uninstalled() {
	logDebug "uninstalling app"
	for (device in getChildDevices())
	{
		deleteChildDevice(device.deviceNetworkId)
	}
}

def updated() {	
    logDebug "Updated with settings: ${settings}"
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
	subscribe(luxSensors, "illuminance", luxHandler)
    createOrUpdateChildDevice()
    luxHandler()	
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def luxHandler(evt) {
    log.info "Lux sensor change; checking totals..."
    def totalCount = luxSensors.size()
    def device = getChildDevice(state.luxDevice)
    device.sendEvent(name: "TotalCount", value: totalCount)    
    def listLux = []
    def avgLux = 0
    def total = 0
    luxSensors.each {it ->
        total = (total + it.currentIlluminance)
        listLux.add(it.currentIlluminance)
    }
    avgLux = (total / totalCount).toDouble().round()
    logDebug "Average lux is ${avgLux}"
    device.sendEvent(name: "AverageLux", value: avgLux)
    minLux = listLux.min()
    logDebug "Min lux is ${minLux}"
    device.sendEvent(name: "MinLux", value: minLux)
    maxLux = listLux.max()
    logDebug "Max lux is ${maxLux}"
    device.sendEvent(name: "MaxLux", value: maxLux)
}


def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("luxgroup:" + app.getId())
    if (!childDevice || state.luxDevice == null) {
        logDebug "Creating child device"
        state.luxDevice = "luxgroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "luxgroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
    }
}

def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
}