/**
 *
 * Sensor Groups+_Temp
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
    name: "Sensor Groups+_Temp",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of temp sensors.",
    category: "Convenience",
    parent: "rle.sg+:Sensor Groups+",
    iconUrl: "",
    iconX2Url: "")

preferences {
    page(name: "prefTempGroup")
    page(name: "prefSettings")
}

def prefTempGroup() {
	return dynamicPage(name: "prefTempGroup", title: "Create a Temp Group", nextPage: "prefSettings", uninstall:true, install: false) {
		section {
            label title: "<b>***Enter a name for this child app.***</b>"+
            "<br>This will create a virtual temp sensor which reports the high, low, and average temp based on the sensors you select.", required:true
		}
	}
}

def prefSettings() {
	return dynamicPage(name: "prefSettings", title: "", install: true, uninstall: true) {
		section {
			paragraph "<b>Please choose which sensors to include in this group.</b>"

			input "tempSensors", "capability.temperatureMeasurement", title: "Temp sensors to monitor", multiple:true, required:true
            
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
	subscribe(tempSensors, "temp", tempHandler)
    createOrUpdateChildDevice()
    tempHandler()	
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def tempHandler(evt) {
    log.info "Temp sensor change; checking totals..."
    def totalCount = tempSensors.size()
    def device = getChildDevice(state.tempDevice)
    device.sendEvent(name: "TotalCount", value: totalCount)    
    def listTemp = []
    def avgTemp = 0
    def total = 0
    tempSensors.each {it ->
        total = (total + it.currentTemperature)
        listTemp.add(it.currentTemperature)
    }
    avgTemp = (total / totalCount).toDouble().round(1)
    logDebug "Average temp is ${avgTemp}"
    device.sendEvent(name: "AverageTemp", value: avgTemp)
    minTemp = listTemp.min()
    logDebug "Min temp is ${minTemp}"
    device.sendEvent(name: "MinTemp", value: minTemp)
    maxTemp = listTemp.max()
    logDebug "Max temp is ${maxTemp}"
    device.sendEvent(name: "MaxTemp", value: maxTemp)
}


def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("tempgroup:" + app.getId())
    if (!childDevice || state.tempDevice == null) {
        logDebug "Creating child device"
        state.tempDevice = "tempgroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "tempgroup:" + app.getId(), 1234, [name: app.label + "_device", isComponent: false])
    } else if (childDevice && childDevice.name != app.label)
		childDevice.name = app.label + "_device"
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