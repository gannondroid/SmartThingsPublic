preferences {
	input("username",	"text",		title: "Camera username",	description: "Camera username for camera.", autoCorrect:false)
	input("password",	"password",	title: "Camera password",	description: "Camera password for camera.", autoCorrect:false)
    input("channel",   "channel",	title: "Camera Channel",	description: "Camera channel IE 101 201 301 401", autoCorrect:false)
    input("ip",			"text",		title: "IP or Hostname",	description: "WAN IP address or Hostname (minus - http://)", autoCorrect:false)
	input("port",		"number",	title: "Port",				description: "WAN Port number")}   

metadata {
definition (name: "WAN IP Camera v2", namespace: "WAN IP Camera v2", author: "Drew/Dlasher") {
	capability "Image Capture"
    capability "Actuator"
	capability "Switch"
}

tiles {

	standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
	  state "default", label: 'Take', action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
	}

	standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: false, canChangeBackground: false, decoration: "flat") {
		state "take", label: "Take Photo", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
		state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#00ff00"
		state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
    }
    /*standardTile("motion", "device.motionStatus", inactiveLabel: false, decoration: "flat") {
    	state "off", label: "off", action: "toggleMotion", icon: "st.motion.motion.inactive", backgroundColor: "#FFFFFF"
		state "on", label: "on", action: "toggleMotion", icon: "st.motion.motion.active",  backgroundColor: "#00ff00"
    }
    standardTile("refresh", "device.cameraStatus", inactiveLabel: false, decoration: "flat") {
        state "refresh", action:"polling.poll", icon:"st.secondary.refresh"
    }*/
    carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

	main "camera"
	details([
    "take", 
    //"motion", 
    //"refresh", 
    "cameraDetails"])    
}
}
def parse(String description) {
log.debug "Parsing '${description}'"
}

def parseCameraResponse(def response) {
log.debug( "parseCameraResponse() started" );

if(response.headers.'Content-Type'.contains("image/jpeg")) {
	def imageBytes = response.data

	if(imageBytes) {
		storeImage(getPictureName(), imageBytes)
	}
} else {
	log.error("${device.label} could not capture an image.")
}
}

def getPictureName() {
log.debug( "getPictureName() started" );

def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
"image" + "_$pictureUuid" + ".jpg"
}

def take() {
log.debug( "take() started" );

log.debug("${device.label} taking photo")

def strUrl = "http://${username}:${password}@${ip}:${port}/Streaming/channels/${channel}/picture";
log.debug( "strUrl = ${strUrl}" );

httpGet( strUrl ){
	response -> log.info("${device.label} image captured")
	parseCameraResponse(response)
}
}
/*
def motion() {
log.debug( "motion() state" );

def strUrl = "http://${username}:${password}@${ip}:${port}/MotionDetection/1{}";
log.debug( "strUrl = ${strUrl}" );

httpPost( strUrl ){

}
}

def motionDisable = new XmlSlurper().parseText('''
<MotionDetection xmlns="http://www.w3.org/1999/xhtml/" version="1.0">
<id>1</id>
<enabled>false</enabled>
</MotionDetection>
   ''')
def motionEnable = new XmlSlurper().parseText('''
<MotionDetection xmlns="http://www.w3.org/1999/xhtml/" version="1.0">
<id>1</id>
<enabled>True</enabled>
</MotionDetection>
   ''')

Because the interface gives lots of detail (about the defined regions), you will probably find it easiest to do what I described above.  Get the existing settings like this:
Code: [Select]
curl http://user:password@ipaddress/MotionDetection/1 >resp.xml

Then simply edit the resp.xml file downloaded by that command (and save it as, for example: motionoff.xml) and change  the top <enabled>true</enabled> to <enabled>false</enabled>, then send it back up again with:
Code: [Select]
curl -T motionoff.xml  http://user:password@ipaddress/MotionDetection/1

To turn it back on, send a version of the file with enabled set to true.  Just remember to update your    scripts if you change the region on the camera (because the XML also contains the region definitions).
*/