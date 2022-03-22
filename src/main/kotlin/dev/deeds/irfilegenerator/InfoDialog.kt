/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.deeds.irfilegenerator

import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.tooltip
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class InfoDialog : Dialog() {

    fun genP(text: String) : Paragraph {
        val para = p(text)
        para.style.set("margin-top", "0.1em").set("margin-bottom", "0.1em")
        return para
    }

    init {
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        verticalLayout {
            isPadding = false
            isSpacing = false
            isMargin = false
            alignItems = FlexComponent.Alignment.STRETCH

            horizontalLayout(classNames = "dense-para"){
                button() {
                    icon = Icon(VaadinIcon.CLOSE)
                    onLeftClick {
                        close()
                    }
                }
            }
            h2("Instructions")
            p("First, grab your IR receiver and the remote you want to use!") {
                classNames.add("dense-para")
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
            }
            p("If you know the manufacturer of your remote, choose it from the drop down menu. If you don't know, no worries!") {
                classNames.add("dense-para")
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
            }
            p("Open the IR app on your receiver and hit 'Learn new remote'") {
                classNames.add("dense-para")
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
            }
            p("Press a button on your remote, and 3 things will pop-up: The protocol (in bold), the device address ('A'), and the command ID ('C')") {
                classNames.add("dense-para")
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
            }
            p("Copy these into the Protocol, Device ID, and Function ID boxes. Don't worry about typing the '0x' part, it's already there for you. Once it's filled out, hit 'Add to table'") {
                classNames.add("dense-para")
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
            }
            p("You should see the number of potential devices go down drastically. Don't worry if there's still more than one— try again with a few different buttons on the remote to narrow things down") {
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
            }

            h2("License stuff")
            p("Contains/accesses irdb by Simon Peter and contributors, used under permission. For licensing details and for information on how to contribute to the database, see ") {
                classNames.add("dense-para")
                style.set("margin-top", "0px").set("margin-bottom", "0.05em")
                html("<a href=\"https://github.com/probonopd/irdb\">https://github.com/probonopd/irdb</a>")
            }
        }
    }
}