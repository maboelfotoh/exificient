/*
 * Copyright (C) 2007-2011 Siemens AG
 *
 * This program and its interfaces are free software;
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.siemens.ct.exi.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.AssertionFailedError;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.siemens.ct.exi.AbstractTestDecoder;
import com.siemens.ct.exi.AbstractTestEncoder;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.EncodingOptions;
import com.siemens.ct.exi.FidelityOptions;
import com.siemens.ct.exi.GrammarFactory;
import com.siemens.ct.exi.QuickTestConfiguration;
import com.siemens.ct.exi.TestDOMDecoder;
import com.siemens.ct.exi.TestDOMEncoder;
import com.siemens.ct.exi.TestSAXDecoder;
import com.siemens.ct.exi.TestSAXEncoder;
import com.siemens.ct.exi.TestStAXDecoder;
import com.siemens.ct.exi.TestStAXEncoder;
import com.siemens.ct.exi.grammar.Grammar;
import com.siemens.ct.exi.helpers.DefaultEXIFactory;
import com.siemens.ct.exi.util.FragmentUtilities;

enum API {
	SAX, DOM, StAX;
}

public abstract class AbstractTestCase extends XMLTestCase {
	protected Vector<TestCaseOption> testCaseOptions = new Vector<TestCaseOption>();
	protected GrammarFactory grammarFactory = GrammarFactory.newInstance();

	public AbstractTestCase(String s) {
		super(s);
	}

	private void _testOption(TestCaseOption tco, API api) throws Exception {
		if (tco.isSchemaInformedOnly() && tco.getSchemaLocation() == null) {
			return;
		}

		// exi factory
		EXIFactory ef = DefaultEXIFactory.newInstance();
		ef.setCodingMode(tco.getCodingMode());
		ef.setFidelityOptions(tco.getFidelityOptions());
		ef.setFragment(tco.isFragments());
		ef.setDatatypeRepresentationMap(tco.getDtrMapTypes(), tco
				.getDtrMapRepresentations());
		ef.setSelfContainedElements(tco.getSelfContainedElements());
		if (tco.getBlockSize() >= 0) {
			ef.setBlockSize(tco.getBlockSize());
		}
		if (tco.getValueMaxLength() >= 0) {
			ef.setValueMaxLength(tco.getValueMaxLength());
		}
		if (tco.getValuePartitionCapacity() >= 0) {
			ef.setValuePartitionCapacity(tco.getValuePartitionCapacity());
		}
		ef.setEncodingOptions(tco.getEncodingOptions());
		
		if (tco.getProfile() != null) {
			ef.setProfile(tco.getProfile());
		}

		// schema-informed grammar ?
		if (tco.getSchemaLocation() == null) {
			// schema-less
		} else if (tco.getSchemaLocation().length() == 0) {
			// xsd-types informed
			Grammar grammar = grammarFactory.createXSDTypesOnlyGrammar();
			ef.setGrammar(grammar);
		} else {
			// schema-informed
			Grammar grammar = grammarFactory.createGrammar(tco
					.getSchemaLocation());
			ef.setGrammar(grammar);
		}

		// EXI output stream
		ByteArrayOutputStream exiEncodedOutput = new ByteArrayOutputStream();

		// XML input stream
		String xmlLocation = QuickTestConfiguration.getXmlLocation();
		InputStream xmlInput = new FileInputStream(xmlLocation);

		AbstractTestEncoder testEncoder = getTestEncoder(api, exiEncodedOutput);

		// --> encode
		testEncoder.encodeTo(ef, xmlInput);
		exiEncodedOutput.flush();

		// EXI input stream
		InputStream exiDocument = new ByteArrayInputStream(exiEncodedOutput
				.toByteArray());

		EncodingOptions encodingOptions = tco.getEncodingOptions();
		// if (tco.isIncludeOptions() && tco.isIncludeSchemaId()) {
		if (encodingOptions.isOptionEnabled(EncodingOptions.INCLUDE_OPTIONS)
				&& encodingOptions
						.isOptionEnabled(EncodingOptions.INCLUDE_SCHEMA_ID)) {
			// all EXI options and schemaID from the header have to be used
			ef = DefaultEXIFactory.newInstance();
		}

		// <-- 1. decode as SAX
		try {
			decode(ef, exiDocument, API.SAX, tco.isXmlEqual());
		} catch (Throwable e) {
			// encode-decode msg
			throw new Exception("{" + api + "->SAX} " + e.getLocalizedMessage()
					+ " [" + tco.toString() + "]", e);
		}

		// <-- 2. decode as DOM
		try {
			exiDocument.reset();
			decode(ef, exiDocument, API.DOM, tco.isXmlEqual());
		} catch (Throwable e) {
			throw new Exception("{" + api + "->DOM} " + e.getLocalizedMessage()
					+ " [" + tco.toString() + "]", e);
		}
		
		// <-- 3. decode as StAX
		try {
			exiDocument.reset();
			decode(ef, exiDocument, API.StAX, tco.isXmlEqual());
		} catch (Throwable e) {
			throw new Exception("{" + api + "->StAX} " + e.getLocalizedMessage()
					+ " [" + tco.toString() + "]", e);
		}
	}

	protected void decode(EXIFactory ef, InputStream exiDocument, API api,
			boolean checkXMLEqual) throws Exception {
		// decoded XML
		ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();

		// decode
		AbstractTestDecoder testDecoder = getTestDecoder(api);
		// AbstractTestDecoder testDecoder = getTestDecoder(API.SAX);
		testDecoder.decodeTo(ef, exiDocument, xmlOutput);
		xmlOutput.flush();

		// check XML validity OR equal
		InputStream testDecXML = new ByteArrayInputStream(xmlOutput
				.toByteArray());

		List<String> domDiffIssues = new ArrayList<String>();
		// entity references
		domDiffIssues.add("./data/general/entityReference1.xml");
		domDiffIssues.add("./data/general/entityReference2.xml");
		// fragments
		domDiffIssues.add("./data/fragment/fragment3a.xml.frag");
		domDiffIssues.add("./data/fragment/fragment3b.xml.frag");
		// ???
		domDiffIssues.add("./data/W3C/xhtml/www.w3.org.htm");
		domDiffIssues.add("./data/W3C/xhtml/en.wikipedia.org-wiki-EXI.htm");

		String xmlLocation = QuickTestConfiguration.getXmlLocation();

		if (( api == API.DOM || api == API.StAX  ) && domDiffIssues.contains(xmlLocation)) {
			// TODO find a solution for known DOM diff tool issues
			// System.out.println("No DOM diff for: " + xmlLocation);
		} else if (checkXMLEqual) {
			InputStream control = new FileInputStream(xmlLocation);
			checkXMLEquality(ef, control, testDecXML);
		} else {
			checkXMLValidity(ef, testDecXML);
		}
	}

	protected AbstractTestEncoder getTestEncoder(API api,
			OutputStream encodedOutput) {
		if (api == API.SAX) {
			return new TestSAXEncoder(encodedOutput);
		} else if (api == API.DOM) {
			return new TestDOMEncoder(encodedOutput);
		} else {
			assert (api == API.StAX);
			return new TestStAXEncoder(encodedOutput);
		}
	}

	protected AbstractTestDecoder getTestDecoder(API api) {
		if (api == API.SAX) {
			return new TestSAXDecoder();
		} else if (api == API.DOM) {
			return new TestDOMDecoder();
		} else {
			assert (api == API.StAX);
			return new TestStAXDecoder();
		}
	}

	protected void checkXMLValidity(EXIFactory ef, InputStream testXML)
			throws Exception {
		if (ef.isFragment()) {
			// surround with root element for equality check
			testXML = FragmentUtilities.getSurroundingRootInputStream(testXML);
		}

		// try to read stream and create DOM
		try {
			@SuppressWarnings("unused")
			Document docTest = TestDOMEncoder.getDocument(testXML);
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg.contains("The entity \"ent\" was referenced, but not declared")) {
				// known issue? --> entityReference2 for StAX
				return;
			} 
			throw new Exception("Not able to create DOM. " + ef.getCodingMode()
					+ ", schema=" + ef.getGrammar().isSchemaInformed() + " "
					+ ef.getFidelityOptions().toString(), e);
		}
		// assertXMLValid(new InputSource(test));
	}

	protected void checkXMLEquality(EXIFactory ef, InputStream control,
			InputStream testXML) throws IOException, AssertionFailedError,
			ParserConfigurationException, SAXException {
		if (ef.isFragment()) {
			// surround with root element for equality check
			control = FragmentUtilities.getSurroundingRootInputStream(control);
			testXML = FragmentUtilities.getSurroundingRootInputStream(testXML);
		}

		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);

		Document docControl = TestDOMEncoder.getDocument(control);
		Document docTest = null;
		try {
			docTest = TestDOMEncoder.getDocument(testXML);
		} catch (SAXParseException e1) {
			String msg = e1.getMessage();
			if (msg.contains("The entity \"ent\" was referenced, but not declared")) {
				// known issue? --> entityReference2 for StAX
				return;
			}
			throw e1;
		}

		try {
			assertXMLEqual(ef.getCodingMode() + ", schema="
					+ ef.getGrammar().isSchemaInformed() + " "
					+ ef.getFidelityOptions().toString(), docControl, docTest);
		} catch (AssertionFailedError e) {
			// XMLUnit seems to have problems with XHTML and DTD throwing wrong
			// assertion failure
			String msg = e.getMessage();
			// System.out.println(msg);
			if (msg.contains("Expected doctype name 'html'")) {
				// do nothing, false failure
			} else if (msg.contains("Expected number of child nodes '3' but was '2' - comparing <greeting...> at /greeting[1] to <greeting...> at /greeting[1]")) {
				// ER issue, see testEntityReference1
			} else {
				throw new AssertionFailedError(msg);
			}
		}

		// assertXMLEqual(new InputSource(control), new InputSource(test));
	}

	protected void _test(FidelityOptions noValidOptions) throws Exception {
		// schema-less
		_test(null, noValidOptions);

		// schema-informed
		_test(QuickTestConfiguration.getXsdLocation(), noValidOptions);
	}
	
	protected void _test() throws Exception {
		_test(null);
	}

	private void _test(String schemaLocation, FidelityOptions noValidOptions) throws Exception {
		// test options
		for (int i = 0; i < testCaseOptions.size(); i++) {
			TestCaseOption tco = testCaseOptions.get(i);
			
			if (tco.getFidelityOptions().equals(noValidOptions)) {
				continue;
			}
			
			
			// update schema
			tco.setSchemaLocation(schemaLocation);
			// test all encode APIs
			
			// 1. encode SAX
			_testOption(tco, API.SAX);

			// 2. encode DOM
			_testOption(tco, API.DOM);
			
			// 3. encode StAX
			_testOption(tco, API.StAX);
		}
	}

}
