/**
 *  Alexa and Lambda - Perform an operation on a device as requested by Alexa
 *
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  4/20/16 KHD (n8xd) test jig for processing device and op commands sent through Alexa/Lambda
 */
 
definition(
    name: "Alexa and Lambda",
    namespace: "n8xd",
    author: "n8xd",
    description: "Do what Alexa tells us to do",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section(title: "Select Devices") {
        input "light", "capability.switch", title: "Select a light or outlet", required: true, multiple:false
        input "pres","capability.presenceSensor", title: "Select a Presence Sensor", required: true, multiple:false
    }
}


def installed() {}

def updated() {}



mappings {
    // test central command GET for Alexa/Lambda
    path("/:device/:operator"){
        action: [
            GET: "centralCommand",
        ]
    } 
 }

// take a device and operation the user has spoken to Alexa, perform the opreation on the device and confirm with words.
def centralCommand() {
        log.debug params
	    def dev = params.device
        def op = params.operator
        log.trace "Central Command ${dev} ${op}"
        
        if (dev == "keiths desk lamp") {
            def arg = light.currentState("switch")["value"]  //value before change 
            if (op == "on") { light.on(); arg = op;}         //switch flips slow in state, so tell them we did Op
            else if (op == "off") { light.off(); arg = op; } // ...or it will report what it was, not what we want
            else if (op == "status") { }                     // dont report Op, report the real currentState
            return ["talk2me":"${dev} is ${arg}" ]           // talk2me : keiths desk lamp is on (or off) 
        } 
        
        if (dev == "keiths medic pendant") {
            def arg = pres.currentState("presence")["value"] // lookup the current presence status
            return ["talk2me":"${dev} is ${arg}"]            // talk2me : keiths medic pendant is present (or not present)
        } 
        
        // default return
        def ok = ["talk2me":"I cant figure out how to do ${op} with ${dev}" ]
        return ok
}