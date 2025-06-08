package net.pitan76.mpltemplateideplugin.util

import java.io.File
import java.time.Year

fun generateLicense(basePath: String, license: String, author: String) {
    val licenseTemplates = mapOf(
        "MIT" to """
            MIT License
            
            Copyright (c) ${Year.now()} $author
            
            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:
            
            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.
            
            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
            SOFTWARE.
        """.trimIndent(),
        "Apache-2.0" to """
            Apache License 2.0
            
            Copyright ${Year.now()} $author
            
            Licensed under the Apache License, Version 2.0 (the "License");
            you may not use this file except in compliance with the License.
            You may obtain a copy of the License at
            
                http://www.apache.org/licenses/LICENSE-2.0
            
            Unless required by applicable law or agreed to in writing, software
            distributed under the License is distributed on an "AS IS" BASIS,
            WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            See the License for the specific language governing permissions and
            limitations under the License.
        """.trimIndent(),
        // 他のライセンスも同様に追加
        "GPL-3.0" to """
            GNU GENERAL PUBLIC LICENSE
            Version 3, 29 June 2007
            
            Copyright (C) ${Year.now()} $author
            
            This program is free software: you can redistribute it and/or modify
            it under the terms of the GNU General Public License as published by
            the Free Software Foundation, either version 3 of the License, or
            (at your option) any later version.
            
            This program is distributed in the hope that it will be useful,
            but WITHOUT ANY WARRANTY; without even the implied warranty of
            MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
            GNU General Public License for more details.
            
            You should have received a copy of the GNU General Public License
            along with this program. If not, see <http://www.gnu.org/licenses/>.
        """.trimIndent(),
        "All Rights Reserved" to """
            All Rights Reserved.
            
            Copyright (c) ${Year.now()} $author
        """.trimIndent(),
        "Custom" to """
            Please define your custom license here.
        """.trimIndent()
    )

    val licenseContent = licenseTemplates[license] ?: "No license selected."
    val licenseFile = File(basePath, "LICENSE")
    licenseFile.writeText(licenseContent)
}
