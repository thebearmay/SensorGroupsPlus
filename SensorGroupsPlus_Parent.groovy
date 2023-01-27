/**
 *
 * Sensor Groups+
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
 * v2.0		RLE		Starting fresh with the change log.
 * v2.0.1	RLE		Now with more colors!
 * v2.1.0	RLE		New app for grouping locks.
 */
 
definition(
    name: "Sensor Groups+",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of sensors.",
    category: "Convenience",
	iconUrl: "",
    iconX2Url: "")

preferences {
     page(name: "mainPage", title: "", install: true, uninstall: true)
} 

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
	runIn(1800,logsOff)
}

def mainPage() {
    dynamicPage(name: "mainPage") {
    	isInstalled()
        getAppsList()
        def childApps = ["Sensor Groups+_CO",
			"Sensor Groups+_Contact",
			"Sensor Groups+_Humidity",
			"Sensor Groups+_Motion",
			"Sensor Groups+_Smoke",
			"Sensor Groups+_Switch",
			"Sensor Groups+_Temp",
			"Sensor Groups+_Water",
			"Sensor Groups+_Lux",
			"Sensor Groups+_Power",
			"Sensor Groups+_Lock"]
		logDebug "Installed apps are ${state.allAppNames}"
		if(state.appInstalled == 'COMPLETE'){
			section(getFormat("header","${app.label}")) {
				paragraph getFormat("important","Provides options for combining multiple sensors into a single device to provide combined updates.")
			}
			section(getFormat("important2Bold","Child Apps")) {
                childApps.sort().each { kid ->
                    if (kid in state.allAppNames) {
                        app(name: kid+"App+", appName: kid, namespace: "rle.sg+", title: getFormat("lessImportant","Add a new ${kid} Instance"), multiple: true)
                    } else {
                    logDebug "${kid} not installed."
				    }
                }
			}
			section(getFormat("header","Options")) {
       			label title: getFormat("important","Enter a name for this parent app (optional)"), required: false
				input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
 			}
		}
	}
}

def isInstalled() {
	state.appInstalled = app.getInstallationState() 
	if (state.appInstalled != 'COMPLETE') {
		section
		{
			paragraph "Please click <b>Done</b> to install the parent app."
		}
  	}
}

def getAppsList() {        
	def params = [
		uri: "http://127.0.0.1:8080/app/list",
		textParser: true,
		headers: [
			Cookie: state.cookie
		]
	  ]
	
	def allAppNames = []
	try {
		httpGet(params) { resp ->
			def matcherText = resp.data.text.replace("\n","").replace("\r","")
			def matcher = matcherText.findAll(/(<tr class="app-row" data-app-id="[^<>]+">.*?<\/tr>)/).each {
				def allFields = it.findAll(/(<td .*?<\/td>)/)
				def title = allFields[0].find(/title="([^"]+)/) { match,t -> return t.trim() }
                allAppNames.add(title)
			}
		}
	} catch (e) {
		log.error "Error retrieving installed apps: ${e}"
        log.error(getExceptionMessageWithLine(e))
	}
    state.allAppNames = allAppNames.sort()
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
	if(type == "red") return "<div style='color:#660000'>${myText}</div>"
	if(type == "importantBold") return "<div style='color:#32a4be;font-weight: bold'>${myText}</div>"
	if(type == "important") return "<div style='color:#32a4be'>${myText}</div>"
	if(type == "important2") return "<div style='color:#5a8200'>${myText}</div>"
	if(type == "important2Bold") return "<div style='color:#5a8200;font-weight: bold'>${myText}</div>"
	if(type == "lessImportant") return "<div style='color:green'>${myText}</div>"
	if(type == "rateDisplay") return "<div style='color:green; text-align: center;font-weight: bold'>${myText}</div>"
}