/* **DISCLAIMER**
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
* 1. the software will meet your requirements or expectations;
* 2. the software or the software content will be free of bugs, errors, viruses or other defects;
* 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
* 4. the software will be compatible with third party software;
* 5. any errors in the software will be corrected.
* The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
*/ 

def clientVersion() {
    return "05.04.00"
}

/**
* Blink Camera
*
* Copyright RBoy, redistribution of code is not allowed without permission
* Change log:
* 2017-1-12 (v5.4.0) Updated UI to include setting video length and system status in sync with blink app
* 2016-11-16 - Updated video recording length to a maximum of 120 seconds
* 2016-10-23 - Added ability to check for new code versions automatically once a day
* 2016-9-13 - Reducing logging from queued actions, commands for setMotionSensitivity and setRetriggerTime with attributes motionSensitivity and retriggerTime
* 2016-8-19 - Changed the layout to fix the temperature tile now showing bug on Android devices, updated battery icon
* 2016-6-23 - Fix for SHM and random errors for some users
* 2016-5-24 - Disabled imageDataJpeg attribute and storage for now on ST's request
* 2016-5-19 - Execute Camera on/off and EnableAlerts/DisableAlerts commands asynchronously so the ST platform routines don't timeout when doing a large number of cameras
* 2016-4-30 - Added info level logging
* 2016-4-24 - Retry live view once before giving up if server is busy
* 2016-4-23 - Summary is the default tile to show the motion detection on/off status in the list of things
* 2016-4-23 - Added support for Live video streaming
* 2016-4-20 - Added DH version in setup page
* 2016-4-14 - Added support for LED configuration
* 2016-4-14 - Added support to configure video recording length
* 2016-4-12 - Performance optimization, don't need to query network ID each time, save it for future use since it doesn't change
* 2016-4-5 - Added logout contexts for blink server sanity
* 2016-4-5 - Last sensor update made more readable
* 2016-4-3 - Debug stuff
* 2016-4-2 - Force refresh on device install
* 2016-4-2 - Fix for uninitialized queue error
* 2016-3-30 - Added heartbeat system to compensate for dropped schedules and ensure queued actions are completed
* 2016-3-30 - Code clean up
* 2016-3-29 - Changed color to blue and text to Motion instead of Intruder when motion is detected
* 2016-3-22 - Added support to check for sync module going offline and reporting it to the user (don't send commands if sync module is offline)
* 2016-3-15 - Changed imageDataJpeg to a Base64 string instead of UTF-8 to make it compatible with SmartTiles
* 2016-3-15 - Fixed updated function to call refresh automatically after updating settings
* 2016-3-12 - Updated the icons to bring it more inline with the native app
* 2016-3-11 - Added support for imageDataJpeg attribute to report the JPEG picture as a UTF-8 String
* 2016-3-8 - Fix messages
* 2016-3-7 - Debugging stuff added
* 2016-3-4 - Added support to customize switch interface behavior to either control individual cameras or the entire system (sync module)
* 2016-3-3 - Added support for capability Battery
* 2016-3-2 - Switch/siren/default action is now to enable/disable camera. Motion must be enabled at global level 
* 2016-3-1 - Added support for Motion Event notifications through Motion Sensor Interface
* 2016-2-29 - Added support for forcing camera sensors update and showing last time they were updated
* 2016-2-27 - Added support for WiFi and LFR signal
* 2016-2-27 - Fixed issues with camera pictures and referred processing
* 2016-2-26 - Added support for controlling the camera through Rules Machines (monitorOn/monitorOff)
* 2016-2-26 - Added support for temperature
* 2016-2-23 - Initial release
*/

metadata {
    definition (name: "RBoy Blink Camera", namespace: "rboy", author: "RBoy") {
        capability "Polling"
        capability "Image Capture"
        capability "Alarm"
        capability "Relay Switch"
        capability "Switch"
        capability "Refresh"
        capability "Motion Sensor"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Battery"
        capability "Video Camera"
        capability "Video Capture"

        // Custom attributes
        attribute "monitorStatus", "string"
        attribute "enableStatus", "string"
        //attribute "wifi", "number"
        //attribute "lfr", "number"
        attribute "lastUpdate", "string"
        //attribute "imageDataJpeg", "string"
        attribute "videoLength", "number"
        attribute "motionSensitivity", "number"
        attribute "retriggerTime", "number"
        attribute "ledStatus", "string"
        attribute "codeVersion", "string"
        attribute "dhName", "string"

        // Local commands
        command "toggleCamera"
        command "enableCamera"
        command "monitorOn"
        command "monitorOff"
        command "disableCamera"
        command "forceSensorsUpdate"
        command "sendConfigure"
        command "sendQuery"
        command "setVideoLength", ["number"]
        command "setMotionSensitivity", ["number"]
        command "setRetriggerTime", ["number"]
        command "ledOn"
        command "ledOff"
        command "ledAuto"
        command "startVideo"
        //command "setHiRes"
        //command "setLowRes"

        // Calls from Parent to Child
        command "generateEvent", ["JSON_OBJECT"]
        command "log", ["string","string"]
        command "saveImage", ["string"]
        command "deferredLoopbackQueue", ["number","string","string","enum"]
        command "updateSwitchBehavior", ["enum"]
        command "logout"
    }

    preferences {
        input title: "", description: "Blink Device Handler v${clientVersion()}", displayDuringSetup: false, type: "paragraph", element: "paragraph"
    }
    
    tiles(scale: 2) {
        multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 6, height: 4) {
            tileAttribute("device.camera", key: "CAMERA_STATUS") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-hdpan", action: "", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-hdpan", action: "", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-hdpan", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Click here to connect", icon: "st.camera.dlink-hdpan", action: "", backgroundColor: "#F22000")
			}

			tileAttribute("device.errorMessage", key: "CAMERA_ERROR_MESSAGE") {
				attributeState("errorMessage", label: "", value: "", defaultState: true)
			}

			tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-hdpan", backgroundColor: "#79b821")
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-hdpan", backgroundColor: "#ffffff", defaultState: true)
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-hdpan", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Click here to connect", icon: "st.camera.dlink-hdpan", backgroundColor: "#F22000")
			}

            tileAttribute("device.startLive", key: "START_LIVE") {
				attributeState("live", action: "startVideo", defaultState: true)
			}

			tileAttribute("device.stream", key: "STREAM_URL") {
				attributeState("activeURL", defaultState: true)
			}

			/*tileAttribute("device.profile", key: "STREAM_QUALITY") { // TODO: Blink doesn't support profiles right now
				attributeState("hi", label: "Hi-Res", action: "setHiRes", defaultState: true)
				attributeState("low", label: "Low-Res", action: "setLowRes")
			}*/

			/*tileAttribute("device.betaLogo", key: "BETA_LOGO") {
				attributeState("betaLogo", label: "", value: "", defaultState: true)
			}*/
		}
		
        multiAttributeTile(name:"summary", type: "generic", width: 6, height: 4){
            tileAttribute ("device.enableStatus", key: "PRIMARY_CONTROL") {
                attributeState "on", label: "On", action: "disableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png", backgroundColor: "#79B821", nextState:"..."
                attributeState "off", label: "Off", action: "enableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOff.png", backgroundColor: "#FFFFFF", nextState:"..."
                attributeState "alarm", label: "Motion", action: "toggleCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png",  backgroundColor: "#53A7C0", nextState:"..."
                attributeState "...", label: "...", action:"", nextState:"..."
            }
            tileAttribute ("device.temperature", key: "SECONDARY_CONTROL") {
                attributeState("temperature", label:'${currentValue}°',
                               backgroundColors:[
                                   [value: 31, color: "#153591"],
                                   [value: 44, color: "#1e9cbb"],
                                   [value: 59, color: "#90d2a7"],
                                   [value: 74, color: "#44b621"],
                                   [value: 84, color: "#f1d801"],
                                   [value: 95, color: "#d04e00"],
                                   [value: 96, color: "#bc2323"]
                               ]
                              )
            }
        }

        standardTile("monitorStatus", "device.monitorStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "offline", label: "OFFLINE", action: "refresh", icon: "http://smartthings.rboyapps.com/images/SystemOff.png", backgroundColor: "#FF3333"
            state "on", label: "System Armed", action: "monitorOff", icon: "http://smartthings.rboyapps.com/images/SystemOn.png", backgroundColor: "#FFFFFF", nextState:"..."
            state "off", label: "System Disarmed", action: "monitorOn", icon: "http://smartthings.rboyapps.com/images/SystemOff.png",  backgroundColor: "#FFFFFF", nextState:"..."
            state "...", label: "...", action:"", nextState:"..."
        }

        standardTile("enableStatus", "device.enableStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "on", label: "On", action: "disableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png", backgroundColor: "#FFFFFF", nextState:"..."
            state "off", label: "Off", action: "enableCamera", icon: "http://smartthings.rboyapps.com/images/CameraOff.png", backgroundColor: "#FFFFFF", nextState:"..."
            state "alarm", label: "Motion", action: "toggleCamera", icon: "http://smartthings.rboyapps.com/images/CameraOn.png",  backgroundColor: "#53A7C0", nextState:"..."
            state "...", label: "...", action:"", nextState:"..."
        }

        standardTile("wifi", "device.wifi", width: 2, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "0", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_0.png", backgroundColor: "#FFFFFF"
            state "1", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_1.png", backgroundColor: "#FFFFFF"
            state "2", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_2.png", backgroundColor: "#FFFFFF"
            state "3", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_3.png", backgroundColor: "#FFFFFF"
            state "4", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_4.png", backgroundColor: "#FFFFFF"
            state "5", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_5.png", backgroundColor: "#FFFFFF"
        }

        standardTile("lfr", "device.lfr", width: 2, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "0", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a1.png", backgroundColor: "#FFFFFF"
            state "1", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a2.png", backgroundColor: "#FFFFFF"
            state "2", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a3.png", backgroundColor: "#FFFFFF"
            state "3", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a4.png", backgroundColor: "#FFFFFF"
            state "4", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a5.png", backgroundColor: "#FFFFFF"
            state "5", label: "", icon: "http://smartthings.rboyapps.com/images/wifi_a6.png", backgroundColor: "#FFFFFF"
        }

        carouselTile("cameraDetails", "device.image", width: 4, height: 2) { }

        standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "take", label: "Snap", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
            state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
            state "image", label: "Snap", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
        }

        valueTile("lastUpdate", "device.lastUpdate", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state "lastUpdate", label:'Last Update ${currentValue}'
        }

        standardTile("forceUpdate", "device.lastUpdate", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "", label:'Sensor Update', action: "forceSensorsUpdate", icon: "http://smartthings.rboyapps.com/images/sensors.png"
        }

        valueTile("battery", "device.battery", width: 2, height: 2, inactiveLabel: false) {
            state "battery", label:'\n ${currentValue}%', unit: "", icon: "http://smartthings.rboyapps.com/images/battery.png", backgroundColors:[
                [value: 15, color: "#ff0000"],
                [value: 30, color: "#fd4e3a"],
                [value: 50, color: "#fda63a"],
                [value: 60, color: "#fdeb3a"],
                [value: 75, color: "#d4fd3a"],
                [value: 90, color: "#7cfd3a"],
                [value: 99, color: "#55fd3a"]
            ]
        }

        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label:'${currentValue}°',
                  backgroundColors:[
                      [value: 31, color: "#153591"],
                      [value: 44, color: "#1e9cbb"],
                      [value: 59, color: "#90d2a7"],
                      [value: 74, color: "#44b621"],
                      [value: 84, color: "#f1d801"],
                      [value: 95, color: "#d04e00"],
                      [value: 96, color: "#bc2323"]
                  ]
                 )
        }

        standardTile("refresh", "device.status", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("ledStatus", "device.ledStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
          state "auto", label: "Auto", action: "ledOn", icon: "st.lights.multi-light-bulb-on", backgroundColor: "#9ceaf0", nextState:"..."
          state "on", label: "On", action: "ledOff", icon: "st.lights.multi-light-bulb-off", backgroundColor: "#79b821", nextState:"..."
          state "off", label: "Off", action: "ledAuto", icon: "st.lights.multi-light-bulb-off", backgroundColor: "#FFFFFF", nextState:"..."
          state "...", label: "...", action:"", nextState:"..."
        }

        standardTile("cameraSettings", "device.ledStatus", width: 6, height: 1, inactiveLabel: true, decoration: "flat") {
            state "", label: "Camera Settings", action: "", icon: "http://smartthings.rboyapps.com/images/settings.png", backgroundColor: "#0000FF"
        }

        controlTile("videoLength", "device.videoLength", "slider", width: 5, height: 1, inactiveLabel: true, range:"(1..120)") {
            state "videoLength", action:"setVideoLength", label: 'Motion Detection Video Recording Length'
        }

        standardTile("videoLabel", "device.videoLength", width: 1, height: 1, inactiveLabel: true, decoration: "flat") {
            state "videoLength", label: '${currentValue} s', icon: ""
        }

        valueTile("videoLengthComment", "device.ledStatus", width: 5, height: 1, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
            state "videoLength", label: 'Video Recording Length', action: "", icon: "", backgroundColor: "#FFFFFF"
        }

        standardTile("videoLabelComment", "device.ledStatus", width: 1, height: 1, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
            state "videoLength", label: 'Seconds', action: "", icon: "st.Electronics.electronics8", backgroundColor: "#FFFFFF"
        }

        standardTile("blank", "device.ledStatus", width: 2, height: 1, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
            state "blank", label: "", action: "", icon: "", backgroundColor: "#FFFFFF"
        }

        standardTile("blank2x", "device.ledStatus", width: 2, height: 2, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
            state "blank", label: "", action: "", icon: "", backgroundColor: "#FFFFFF"
        }

        standardTile("sendConfigure", "device.ledStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "", label:'Send Configure', action: "sendConfigure"
        }

        standardTile("sendQuery", "device.ledStatus", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
            state "", label:'Send Query', action: "sendQuery"
        }

        main "summary", "videoPlayer"
        //details(["cameraDetails", "take", "monitorStatus", "enableStatus", "temperature", "refresh", "blank2x", "battery", "videoPlayer", "videoLabelComment", "videoLengthComment", "videoLabel", "videoLength", "lfr", "forceUpdate", "ledStatus", "wifi"])
        details(["videoPlayer", "enableStatus", "refresh", "monitorStatus", "temperature", "forceUpdate", "battery", "cameraDetails", "take"])
    }
}

def initialize() {
    log.trace "Initialize called settings: $settings"
    try {
        if (!state.init) {
            state.init = true
        }
        response(refresh()) // Get the updates
    } catch (e) {
        log.warn "updated() threw $e"
    }
}

def updated() {
    log.trace "Update called settings: $settings"
    try {
        if (!state.init) {
            state.init = true
        }
        response(refresh()) // Get the updates
    } catch (e) {
        log.warn "updated() threw $e"
    }
}

private getPictureName() {
    def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
    "image" + "_$pictureUuid" + ".jpg"
}

//START VIDEO
// Thank you for the tip @ahndee
mappings {
    path("/getInHomeURL") {
        action:
            [GET: "getInHomeURL"]
    }
    
    path("/getOutHomeURL") {
        action:
            [GET: "getOutHomeURL"]
    }
}

def getInHomeURL() {
    log.trace "Called getInHomeURL, returning $state.uri"
    state.uri ? [InHomeURL: state.uri]: null // return null if it's not initialized otherwise ST app crashes
}

def getOutHomeURL() {
    log.trace "Called getOutHomeURL, returning $state.uri"
    state.uri ? [OutHomeURL: state.uri] : null // return null if it's not initialized otherwise ST app crashes
}

// TODO: Blink doesn't support resolutions yet
/*def setHiRes() {
    log.debug "Setting hi resolution stream"
    sendEvent(name: "profile", value: "hi", displayed: false)
}

def setLowRes() {
    log.debug "Setting low resolution stream"
    sendEvent(name: "profile", value: "low", displayed: false)
}*/

def startVideo() {
    //def hiRes = (device.currentValue("profile") == "hi" ? true : false) // default to lowRes
	log.debug "Starting video streaming"

    def uri = parent.getLiveViewURL(this) // Enable live view and get the URL
    if (!uri) { // If it's null then we have a problem
        log.warn "Likely server or sync module busy, trying to activate and get live view URL again"
        uri = parent.getLiveViewURL(this) // Enable live view and get the URL
        if (!uri) { // If it's null then we have a problem
            log.error "Unable to enable live view"
            return
        }
    }
    
    log.trace "Fetching video from: $uri"
    
    state.uri = uri // Save it for the callback
    
	def dataLiveVideo = [
		OutHomeURL  : uri, // This appears to be only one used
		InHomeURL   : uri, // TODO: What is this??
		ThumbnailURL: "http://cdn.device-icons.smartthings.com/camera/dlink-hdpan@2x.png",
		cookie      : [key: "key", value: "value"]
	]

    log.trace "Video settings: $dataLiveVideo"
    
	def event = [
		name           : "stream",
		value          : groovy.json.JsonOutput.toJson(dataLiveVideo).toString(),
		data		   : groovy.json.JsonOutput.toJson(dataLiveVideo),
		descriptionText: "Starting the live video stream",
		eventType      : "VIDEO",
		displayed      : false,
		isStateChange  : true
	]
    
	sendEvent(event)
}
//END START VIDEO

//TAKE PICTURE
def take() {
    log.debug("Taking Photo")
    def sent = parent.takePicture(this)
    log.trace "Take picture command sent successfully: $sent"
}
//END TAKE PICTURE

//SWITCH ACTIONS
def on() {
    log.debug "On requested, enabling camera motion alerts"
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}

def off() {
    log.debug "Off requested, disabling camera motion alerts"
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOff()
    } else {
        disableCamera()
    }
}
//END SWITCH ACTIONS

//ALARM ACTIONS
def both() {
    log.debug "Alarm both requested, enabling camera motion alerts and taking picture"
    take()
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}

def siren() {
    log.debug "Alarm siren requested, enabling camera motion alerts and taking picture"
    take()
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}

def strobe() {
    log.debug "Alarm strobe requested, enabling camera motion alerts and taking picture"
    take()
    log.trace "Checking Switch Interface behavior, Switch Controls System Enable/Disable: $state.switchControlsSystem"
    
    if (state.switchControlsSystem) {
        monitorOn()
    } else {
        enableCamera()
    }
}
//END ALARM ACTIONS

//GLOBAL MONITOR ACTIONS (SYSTEM ACTIVE/INACTIVE)
def toggleMonitor() {
    log.debug "Toggling Monitor"
    if(device.currentValue("monitorStatus") == "off") {
        monitorOn()
    } else {
        monitorOff()
    }
}

def monitorOn() {
    log.debug "Enabling Monitor"
    deferredLoopbackQueue(1, "monitorOn") // defer it by 1 second so that routines don't timeout
    /*def sent = parent.monitorOn(this)
    log.trace "Enable monitor command sent successfully: $sent"*/
}

def monitorOff() {
    log.debug "Disabling Monitor"
    deferredLoopbackQueue(1, "monitorOff") // defer it by 1 second so that routines don't timeout
    /*def sent = parent.monitorOff(this)
    log.trace "Disable monitor command sent successfully: $sent"*/
}
//END MONITOR ACTIONS

//CAMERA MOTION ALERTS ACTIONS
def toggleCamera() {
    log.debug "Toggling Camera"
    if(device.currentValue("enableStatus") == "off") {
        enableCamera()
    } else {
        disableCamera()
    }
}

def disableCamera() {
    log.debug "Disabling Camera Motion Alerts"
    deferredLoopbackQueue(1, "disableAlerts") // defer it by 1 second so that routines don't timeout
    /*def sent = parent.disableAlerts(this)
    log.trace "Disable Camera Motion Alerts sent successfully: $sent"*/
}

def enableCamera() {
    log.debug "Enabling Camera Motion Alerts"
    deferredLoopbackQueue(1, "enableAlerts") // defer it by 1 second so that routines don't timeout
    /*def sent = parent.enableAlerts(this)
    log.trace "Enable Camera Motion Alerts sent successfully: $sent"*/
}
//END ALERTS ACTIONS

//LED ILLUMINATOR CONTROL
def ledOn() {
    log.debug "Turning Illuminator LED On"
    def sent = parent.ledOn(this)
    log.trace "Turning Illuminator LED On sent successfully: $sent"
}

def ledOff() {
    log.debug "Turning Illuminator LED Off"
    def sent = parent.ledOff(this)
    log.trace "Turning Illuminator LED Off sent successfully: $sent"
}

def ledAuto() {
    log.debug "Turning Illuminator LED to Auto"
    def sent = parent.ledAuto(this)
    log.trace "Turning Illuminator LED to Auto sent successfully: $sent"
}
//END LED ILLUMINATOR CONTROL

//VARIOUS STUFF
def setVideoLength(length) {
    log.debug "Setting video recording length to $length seconds"
    if (length < 1) {
        log.warn "Length less than 1 seconds (Blink minimum), may not work"
    } else if (length > 120) {
        log.warn "Length more than 120 seconds (Blink maximum), may not work"
    }
    def sent = parent.setVideoRecordingLength(this, length)
    log.trace "Setting video recording length to $length sent successfully: $sent"
}

def setMotionSensitivity(sensitivity) {
    log.debug "Setting motion sensitivity to $sensitivity"
    if (sensitivity < 1) {
        log.warn "Length less than 1 (Blink minimum), adjusting to 1"
        sensitivity = 1
    } else if (sensitivity > 9) {
        log.warn "Length more than 9 (Blink maximum), adjusting to 9"
        sensitivity = 9
    }
    def sent = parent.setMotionSensitivity(this, sensitivity)
    log.trace "Setting motion sensitivity to $sensitivity sent successfully: $sent"
}

def setRetriggerTime(interval) {
    log.debug "Setting retrigger time to $interval seconds"
    if (interval < 10) {
        log.warn "Length less than 10 seconds (Blink minimum), adjusting to 10 seconds"
        interval = 10
    } else if (interval > 60) {
        log.warn "Length more than 60 seconds (Blink maximum), adjusting to 60 seconds"
        interval = 60
    }
    def sent = parent.setRetriggerTime(this, interval)
    log.trace "Setting retrigger time to $interval sent successfully: $sent"
}
//END VARIOUS STUFF

//DEBUG STUFF
def sendConfigure() {
    log.debug "Sending custom configure"
    def sent = parent.customCommandConfigure("", this) // Enter the custom configure command here
    log.debug "Custom configure command sent successfully: $sent"
}

def sendQuery() {
    log.debug "Sending custom query"
    def sent = parent.customCommandQuery("", this) // Enter the custom query command here
    log.debug "Custom query command sent successfully: $sent"
}
//END DEBUG STUFF

def parse(String description) {
    log.trace "Parse: $description"
}

def refresh() {
    log.trace "Refresh called"
    
    log.trace "Scheduling heartbeat for every 1 minutes"
    schedule("* */1 * * * ?", heartBeat) // Schedule the heartbeat

    poll()
}

def poll() {
    log.trace "Poll called"
    
    def sent = parent.refresh(this)
    log.trace "Refresh command sent successfully: $sent"

    sendEvent(name: "enableStatus", value: device.currentValue("enableStatus"), descriptionText: "Keeping Poll Alive", displayed: false, isStateChange: false) // We need to send something otherwise Poll dies if nothing is done
    device.activity()  // workaround to keep polling from being shut off
    null
}

def forceSensorsUpdate() {
    log.trace "Force sensor update called"
    def sent = parent.forceCameraSensorUpdate(this)
    log.trace "Request to force camera sensors update sent successfully: $sent"
}

def heartBeat() {
	//log.trace "Heartbeat called, checking for pending deferred actions"
    deferredAction()
}

// Deferred action to call the parent with a pending action
def deferredAction() {
    //log.trace "Deferred action called, pending actions: $state.queuedActions"
    Long delay = 99999999 // arbitrary large number to begin with
    def unprocessedActions = []
    def actions

    // Synchronize these lists otherwise we have a race condition
    synchronized(state) {
        if (state.queuedActions == null) { // Initialize it if it doesn't exist
            log.debug "Initializing queued Actions"
            state.queuedActions = [] // initialize it
        }
        actions = state.queuedActions.clone() // make a copy instead of working on original
        //log.warn "BEFORE:$state.queuedActions"
        state.queuedActions.clear() // Clear it
        //log.warn "AFTER:$state.queuedActions"
        //log.warn "PENDING:$actions"
    }
    
    for (action in actions) {
        //log.trace "Processing:$action"
        def now = now()
        if (now >= (action.time + action.delay)) {
            log.trace "Calling parent action: ${action.function} with parameters: ${action.parameters}"
            try {
                if (action.parameters) {
                    parent."${action.function}"(this, *action.parameters, action.retryCount) // Call the parent with the context and parameters
                } else {
                    parent."${action.function}"(this, action.retryCount) // Call the parent with the context
                }
            } catch (e) {
                log.error "Unable to call parent actions, error: $e"
            }
        } else {
            unprocessedActions.add(action)
            delay = Math.min(delay, ((((action.time + action.delay) - now) as Float)/1000).round()) // take smallest pending delay and lets use that
            log.trace "Waiting $delay seconds to process deferred action ${action.function}"
        }
    }

    if (unprocessedActions) { // If anything is pending
        log.trace "Adding unprocessed actions back to queue: $unprocessedActions"
        // Synchronize these lists otherwise we have a race condition
        synchronized(state) {
            state.queuedActions = state.queuedActions + unprocessedActions // Add back any pending actions (since we are adding an array of maps, use + and not << or .add())
            //log.warn "END:$state.queuedActions"
        }
        runIn(delay > 0 ? delay : 1, deferredAction) // defer the loopback action, check for boundary condition
    }
    //log.trace "Deferred action finished, pending actions: $state.queuedActions"
}

// CHILD INTERFACES TO CALL FROM PARENT
// Logout of the Blink server (loop back to close all context related logins)
def void logout() {
    log.trace "Logging out of the Blink server"
    parent.logout(this)
}

// Register the event attributes with the device
def void generateEvent(results) {
    log.trace "Generate Event called: ${results.inspect()}"

    results.each { event ->
        //log.trace "Sending event name: ${event.inspect()}"
        sendEvent(event)
    }
}

// Save the image to the S3 store to display
def void saveImage(image) {
    log.trace "Saving image to S3"

    // Send the image to an App who wants to consume it via an event as a Base64 String - NOTE: Disabled for now since ST has put a limitation on this which is causing trouble with the carousel
    //def bytes = image.buf
    //log.debug "JPEG Data Size: ${bytes.size()}"
    //String str = bytes.encodeBase64()
    //sendEvent(name: "imageDataJpeg", value: str, displayed: false, isStateChange: true)
    //sendEvent(name: "imageDataJpeg", value: "", displayed: false, isStateChange: false) // Wipe it clean so it empties any old data

    // Now save it to the S3 cloud, do this in the end since it removes the data from the object leaving it empty
    storeImage(getPictureName(), image)
}

// Change the default behavior of the switch interface
def void updateSwitchBehavior(switchControlsSystem) {
    log.trace "Updating switch interface behavior, Switch Interface Controls System Enable/Disable: $switchControlsSystem"
    state.switchControlsSystem = switchControlsSystem
}

// Print log message from parent
def void log(message, level = "trace") {
    switch (level) {
        case "trace":
        log.trace "LOG FROM PARENT>" + message
        break;

        case "debug":
        log.debug "LOG FROM PARENT>" + message
        break

        case "info":
        log.info "LOG FROM PARENT>" + message
        break

        case "warn":
        log.warn "LOG FROM PARENT>" + message
        break

        case "error":
        log.error "LOG FROM PARENT>" + message
        break

        default:
            log.error "LOG FROM PARENT>" + message
        break;
    }            
}

// Call back the parent app function after a delay with an option to overwrite existing queued function calls
def void deferredLoopbackQueue(delay, function, parameters = null, overwrite = false, retryCount = 1) {
    log.trace "Deferred loopback called with delay $delay seconds and function $function, parameters $parameters, overwrite $overwrite, retryCount $retryCount"

    sendEvent([name: "codeVersion", value: clientVersion()]) // Save client version for parent app
    sendEvent([name: "dhName", value: "Blink Device Handler"]) // Save DH Name for parent app

   // Save it in the deferral queue
    if (state.queuedActions == null) {
        log.debug "Initializing queued Actions"
        state.queuedActions = [] // initialize it
    }
    //state.queuedActions.clear() // DEBUG CLEAR
    //log.warn "QUEUED ACTIONS: $state.queuedActions" // DEBUG
    
    if (retryCount > 15) {
        log.error "Too many retries, dropping request"
        return
    }
        
    if (overwrite) { // Find if function exists and remove it
        log.trace "Requested to overwrite existing queued action, checking for existing action $function in queued actions $state.queuedActions"
        // Synchronize these lists otherwise we have a race condition
        synchronized(state) {
            def existingAction = state.queuedActions.find { it.function == function }
            if (existingAction) { // We found it
                state.queuedActions.remove(existingAction) // Remove it
                log.trace "Found existing action $existingAction, removed from queued actions $state.queuedActions"
            }
        }
    }
    // Synchronize these lists otherwise we have a race condition
    synchronized(state) {
        state.queuedActions.add([function:function, time:now(), delay:((delay * 1000) as Long), parameters: parameters, retryCount: retryCount]) // Add if to the queue
    }
    
    //state.queuedActions.clear() // DEBUG CLEAR
    //log.warn "QUEUED ACTIONS: $state.queuedActions" // DEBUG

    deferredAction() // defer the loopback action
}