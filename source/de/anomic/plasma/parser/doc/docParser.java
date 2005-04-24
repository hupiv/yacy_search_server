//docParser.java 
//------------------------
//part of YaCy
//(C) by Michael Peter Christen; mc@anomic.de
//first published on http://www.anomic.de
//Frankfurt, Germany, 2005
//
//this file is contributed by Martin Thelian
//last major change: 24.04.2005
//
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//Using this software in any meaning (reading, learning, copying, compiling,
//running) means that you agree that the Author(s) is (are) not responsible
//for cost, loss of data or any harm that may be caused directly or indirectly
//by usage of this softare or this documentation. The usage of this software
//is on your own risk. The installation and usage (starting/running) of this
//software may allow other people or application to access your computer and
//any attached devices and is highly dependent on the configuration of the
//software which must be done by the user of the software; the author(s) is
//(are) also not responsible for proper configuration and usage of the
//software, even if provoked by documentation provided together with
//the software.
//
//Any changes to this file according to the GPL as documented in the file
//gpl.txt aside this file in the shipment you received can be done to the
//lines that follows this copyright notice here, but changes must not be
//done inside the copyright notive above. A re-distribution must contain
//the intact and unchanged copyright notice.
//Contributions and changes to the program code must be marked as such.

package de.anomic.plasma.parser.doc;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import org.textmining.text.extraction.WordExtractor;


import de.anomic.plasma.plasmaParserDocument;
import de.anomic.plasma.parser.Parser;
import de.anomic.plasma.parser.ParserException;

public class docParser implements Parser {

    /**
     * a list of mime types that are supported by this parser class
     */
    public static final HashSet SUPPORTED_MIME_TYPES = new HashSet(Arrays.asList(new String[] {
        new String("application/msword")
    }));     
    
    
	public docParser() {
		super();
	}

	public plasmaParserDocument parse(URL location, String mimeType,
			byte[] source) throws ParserException {
        ByteArrayInputStream contentInputStream = new ByteArrayInputStream(source);
        return this.parse(location,mimeType,contentInputStream);
	}

	public plasmaParserDocument parse(URL location, String mimeType,
			File sourceFile) throws ParserException {
        BufferedInputStream contentInputStream = null;
        try {
            contentInputStream = new BufferedInputStream(new FileInputStream(sourceFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return this.parse(location, mimeType, contentInputStream);
	}

	public plasmaParserDocument parse(URL location, String mimeType,
			InputStream source) throws ParserException {

        
		try {	
			  WordExtractor extractor = new WordExtractor();
			  String contents = extractor.extractText(source);

              plasmaParserDocument theDoc = new plasmaParserDocument(
                      location,
                      mimeType,
                      null,
                      null,
                      null,
                      null,
                      null,
                      contents.getBytes(),
                      null,
                      null);
              
              return theDoc;             
		}
		catch (Exception e) {			
			throw new ParserException("Unable to parse the doc content. " + e.getMessage());
		}        
	}

	public HashSet getSupportedMimeTypes() {
		return docParser.SUPPORTED_MIME_TYPES;
	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
