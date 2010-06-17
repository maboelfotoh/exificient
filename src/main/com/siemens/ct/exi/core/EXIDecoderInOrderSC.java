/*
 * Copyright (C) 2007-2010 Siemens AG
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

package com.siemens.ct.exi.core;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;

import org.xml.sax.helpers.NamespaceSupport;

import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.FidelityOptions;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.grammar.event.EventType;
import com.siemens.ct.exi.values.Value;

/**
 * TODO Description
 * 
 * @author Daniel.Peintner.EXT@siemens.com
 * @author Joerg.Heuer@siemens.com
 * 
 * @version 0.4.20090414
 */

public class EXIDecoderInOrderSC extends EXIDecoderInOrder {

	protected EXIDecoderInOrderSC scDecoder;

	public EXIDecoderInOrderSC(EXIFactory exiFactory) throws EXIException {
		super(exiFactory);
		assert (fidelityOptions.isFidelityEnabled(FidelityOptions.FEATURE_SC));
	}

	@Override
	protected void initForEachRun() throws EXIException, IOException {
		super.initForEachRun();

		// clear possibly remaining decoder
		scDecoder = null;
	}

	public void setInputStream(InputStream is, boolean exiBodyOnly)
			throws EXIException, IOException {
		if (scDecoder == null) {
			super.setInputStream(is, exiBodyOnly);
		} else {
			System.err.println("TODO setInputStream");
		}
	}

	public boolean hasNext() throws EXIException, IOException {
		if ( scDecoder == null ) {
			return super.hasNext();
		} else {
			boolean bool = scDecoder.hasNext();
			if ( this.scDecoder.nextEventType == EventType.END_DOCUMENT ) {		
				scDecoder.decodeEndDocument();
				// Skip to the next byte-aligned boundary in the stream if it is
				// not already at such a boundary
				this.channel.align();
				// indicate that SC portion is over
				scDecoder = null;
				popElement();
				return super.hasNext();
			}
			return bool;
		}
	}

	public EventType next() throws EXIException {
		return (scDecoder == null ? super.next() : scDecoder.next());
	}

	public void decodeStartDocument() throws EXIException {
		if (scDecoder == null) {
			super.decodeStartDocument();
		} else {
			scDecoder.decodeStartDocument();
		}
	}

	public void decodeEndDocument() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeEndDocument();
		} else {
			throw new RuntimeException("[EXI] SC not closed properly?");
		}
	}

	public void decodeStartElement() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeStartElement();
		} else {
			scDecoder.decodeStartElement();
		}
	}

	public void decodeStartElementNS() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeStartElementNS();
		} else {
			scDecoder.decodeStartElementNS();
		}
	}

	public void decodeStartElementGeneric() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeStartElementGeneric();
		} else {
			scDecoder.decodeStartElementGeneric();
		}
	}

	public void decodeStartElementGenericUndeclared() throws EXIException,
			IOException {
		if (scDecoder == null) {
			super.decodeStartElementGenericUndeclared();
		} else {
			scDecoder.decodeStartElementGenericUndeclared();
		}
	}

	public void decodeStartFragmentSelfContained() throws EXIException,
			IOException {
		if (scDecoder == null) {
			//	SC Factory & Decoder
			EXIFactory scEXIFactory = exiFactory.clone();
			scEXIFactory.setEXIBodyOnly(true);
			scEXIFactory.setFragment(true);
			scDecoder = (EXIDecoderInOrderSC) scEXIFactory.createEXIDecoder();
			scDecoder.channel = this.channel;
			scDecoder.setErrorHandler(this.errorHandler);
			scDecoder.initForEachRun();

			// Skip to the next byte-aligned boundary in the stream if it is not
			// already at such a boundary
			this.channel.align();

			// Evaluate the sequence of events (SD, SE(qname), content, ED)
			// according to the Fragment grammar
			scDecoder.decodeStartDocument();
			this.hasNext(); // decode next event
			EventType et = this.next();
			switch (et) {
			case START_ELEMENT:
				scDecoder.decodeStartElement();
				break;
			case START_ELEMENT_GENERIC:
				scDecoder.decodeStartElementGeneric();
				break;
			case START_ELEMENT_GENERIC_UNDECLARED:
				scDecoder.decodeStartElementGenericUndeclared();
				break;
			case START_ELEMENT_NS:
				scDecoder.decodeStartElementNS();
				break;
			default:
				throw new RuntimeException("[EXI] Unsupported EventType " + et
						+ " in SelfContained Element");
			}
		} else {
			// 
			scDecoder.decodeStartFragmentSelfContained();
		}
		

	}

	public void decodeEndElement() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeEndElement();
		} else {
			scDecoder.decodeEndElement();
		}
	}

	public void decodeEndElementUndeclared() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeEndElementUndeclared();
		} else {
			scDecoder.decodeEndElementUndeclared();
		}
	}

	public void decodeAttributeXsiNil() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeAttributeXsiNil();
		} else {
			scDecoder.decodeAttributeXsiNil();
		}
	}

	public void decodeAttributeXsiType() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeAttributeXsiType();
		} else {
			scDecoder.decodeAttributeXsiType();
		}
	}

	public void decodeAttribute() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeAttribute();
		} else {
			scDecoder.decodeAttribute();
		}
	}

	public void decodeAttributeNS() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeAttributeNS();
		} else {
			scDecoder.decodeAttributeNS();
		}
	}

	public void decodeAttributeInvalidValue() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeAttributeInvalidValue();
		} else {
			scDecoder.decodeAttributeInvalidValue();
		}
	}

	public void decodeAttributeAnyInvalidValue() throws EXIException,
			IOException {
		if (scDecoder == null) {
			super.decodeAttributeAnyInvalidValue();
		} else {
			scDecoder.decodeAttributeAnyInvalidValue();
		}
	}

	public void decodeAttributeGeneric() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeAttributeGeneric();
		} else {
			scDecoder.decodeAttributeGeneric();
		}
	}

	public void decodeAttributeGenericUndeclared() throws EXIException,
			IOException {
		if (scDecoder == null) {
			super.decodeAttributeGenericUndeclared();
		} else {
			scDecoder.decodeAttributeGenericUndeclared();
		}
	}

	public void decodeNamespaceDeclaration() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeNamespaceDeclaration();
		} else {
			scDecoder.decodeNamespaceDeclaration();
		}
	}

	public void decodeCharacters() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeCharacters();
		} else {
			scDecoder.decodeCharacters();
		}
	}

	public void decodeCharactersGeneric() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeCharactersGeneric();
		} else {
			scDecoder.decodeCharactersGeneric();
		}
	}

	public void decodeCharactersGenericUndeclared() throws EXIException,
			IOException {
		if (scDecoder == null) {
			super.decodeCharactersGenericUndeclared();
		} else {
			scDecoder.decodeCharactersGenericUndeclared();
		}
	}

	public void decodeDocType() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeDocType();
		} else {
			scDecoder.decodeDocType();
		}
	}

	public void decodeEntityReference() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeEntityReference();
		} else {
			scDecoder.decodeEntityReference();
		}
	}

	public void decodeComment() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeComment();
		} else {
			scDecoder.decodeComment();
		}
	}

	public void decodeProcessingInstruction() throws EXIException, IOException {
		if (scDecoder == null) {
			super.decodeProcessingInstruction();
		} else {
			scDecoder.decodeProcessingInstruction();
		}
	}

	public QName getElementQName() {
		return (scDecoder == null ? super.getElementQName() : scDecoder
				.getElementQName());
	}
	
	public String getStartElementQNameAsString() {
		return (scDecoder == null ? super.getStartElementQNameAsString() : scDecoder
				.getStartElementQNameAsString());
	}
	
	
	public String getEndElementQNameAsString() {
		return (scDecoder == null ? super.getEndElementQNameAsString() : scDecoder
				.getEndElementQNameAsString());
	}

	public QName getAttributeQName() {
		return (scDecoder == null ? super.getAttributeQName() : scDecoder
				.getAttributeQName());
	}
	
	public String getAttributeQNameAsString() {
		return (scDecoder == null ? super.getAttributeQNameAsString() : scDecoder
				.getAttributeQNameAsString());
	}

	public Value getAttributeValue() {
		return (scDecoder == null ? super.getAttributeValue() : scDecoder
				.getAttributeValue());
	}

	public Value getCharactersValue() {
		return (scDecoder == null ? super.getCharactersValue() : scDecoder
				.getCharactersValue());
	}

	public String getDocTypeName() {
		return (scDecoder == null ? super.getDocTypeName() : scDecoder
				.getDocTypeName());
	}

	public String getDocTypePublicID() {
		return (scDecoder == null ? super.getDocTypePublicID() : scDecoder
				.getDocTypePublicID());
	}

	public String getDocTypeSystemID() {
		return (scDecoder == null ? super.getDocTypeSystemID() : scDecoder
				.getDocTypeSystemID());
	}

	public String getDocTypeText() {
		return (scDecoder == null ? super.getDocTypeText() : scDecoder
				.getDocTypeText());
	}

	public String getEntityReferenceName() {
		return (scDecoder == null ? super.getEntityReferenceName() : scDecoder
				.getEntityReferenceName());
	}

	public char[] getComment() {
		return (scDecoder == null ? super.getComment() : scDecoder.getComment());
	}

	public NamespaceSupport getNamespaces() {
		return (scDecoder == null ? super.getNamespaces() : scDecoder
				.getNamespaces());
	}

	public String getPITarget() {
		return (scDecoder == null ? super.getPITarget() : scDecoder
				.getPITarget());
	}

	public String getPIData() {
		return (scDecoder == null ? super.getPIData() : scDecoder.getPIData());
	}
}
