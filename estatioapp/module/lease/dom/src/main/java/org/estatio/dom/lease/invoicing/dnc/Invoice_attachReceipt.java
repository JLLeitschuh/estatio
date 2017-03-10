/*
 *
 *  Copyright 2012-2014 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.dom.lease.invoicing.dnc;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.ActionLayout;
import org.apache.isis.applib.annotation.Contributed;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.Mixin;
import org.apache.isis.applib.annotation.Optionality;
import org.apache.isis.applib.annotation.Parameter;
import org.apache.isis.applib.annotation.ParameterLayout;
import org.apache.isis.applib.annotation.SemanticsOf;
import org.apache.isis.applib.services.clock.ClockService;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.value.Blob;

import org.incode.module.communications.dom.impl.comms.Communication;
import org.incode.module.document.dom.impl.docs.Document;
import org.incode.module.document.dom.impl.docs.DocumentAbstract;
import org.incode.module.document.dom.impl.docs.DocumentRepository;
import org.incode.module.document.dom.impl.docs.DocumentSort;
import org.incode.module.document.dom.impl.docs.DocumentState;
import org.incode.module.document.dom.impl.paperclips.Paperclip;
import org.incode.module.document.dom.impl.paperclips.PaperclipRepository;
import org.incode.module.document.dom.impl.types.DocumentType;
import org.incode.module.document.dom.impl.types.DocumentTypeRepository;

import org.estatio.dom.invoice.Constants;
import org.estatio.dom.invoice.Invoice;
import org.estatio.dom.invoice.paperclips.InvoiceDocAndCommService;

@Mixin
public class Invoice_attachReceipt {

    private final Invoice invoice;

    public Invoice_attachReceipt(final Invoice invoice) {
        this.invoice = invoice;
    }

    @Action(semantics = SemanticsOf.NON_IDEMPOTENT)
    @ActionLayout(contributed = Contributed.AS_ACTION, cssClassFa = "paperclip")
    @MemberOrder(
            name = "documents",
            sequence = "1"
    )
    public Invoice $$(
            final DocumentType documentType,
            @Parameter(fileAccept = "application/pdf")
            @ParameterLayout(named = "Receipt (PDF)")
            final Blob blob,
            @Parameter(optionality = Optionality.OPTIONAL)
            final String fileName
        ) throws IOException {



        //
        // we will automatically attach the receipt doc (once created)
        // to any unsent invoice documents for this Invoice
        // before we do anything, therefore, we get hold of those invoice documents.
        //
        final List<DocumentAbstract> unsentInvoiceDocuments = findUnsentInvoiceDocumentsFor(invoice);

        //
        // now we create the receiptDoc, and attach to the invoice
        //
        String name = determineName(blob, fileName);

        final Document receiptDoc = documentRepository.create(
                documentType, this.invoice.getAtPath(), name, blob.getMimeType().getBaseType());

        // unlike documents that are generated from a template (where we call documentTemplate#render), in this case
        // we have the actual bytes; so we just set up the remaining state of the document manually.
        receiptDoc.setRenderedAt(clockService.nowAsDateTime());
        receiptDoc.setState(DocumentState.RENDERED);
        receiptDoc.setSort(DocumentSort.BLOB);
        receiptDoc.setBlobBytes(blob.getBytes());

        paperclipRepository.attach(receiptDoc, PaperclipRoleNames.INVOICE_RECEIPT, invoice);


        //
        // finally we also attach the newly created receipt doc to the unsent invoices we picked up previously.
        //
        for (DocumentAbstract unsentInvoiceDocument : unsentInvoiceDocuments) {
            paperclipRepository.attach(unsentInvoiceDocument, PaperclipRoleNames.INVOICE_DOCUMENT_SUPPORTED_BY, receiptDoc);
        }

        return invoice;
    }

    private List<DocumentAbstract> findUnsentInvoiceDocumentsFor(final Invoice invoice) {

        final DocumentType invDocType = findDocumentType(Constants.DOC_TYPE_REF_INVOICE);

        final List<DocumentAbstract> unsentInvoiceDocuments = Lists.newArrayList();

        final List<Paperclip> existingInvoicePaperclips = paperclipRepository.findByAttachedTo(invoice);
        for (Paperclip paperclip : existingInvoicePaperclips) {
            final DocumentAbstract document = paperclip.getDocument();
            if(document.getType() == invDocType) {
                boolean sent = whetherSent(document);
                if(!sent) {
                    unsentInvoiceDocuments.add(document);
                }
            }
        }

        return unsentInvoiceDocuments;
    }

    private boolean whetherSent(final DocumentAbstract document) {
        final List<Paperclip> invDocPaperclips = paperclipRepository.findByDocument(document);
        for (final Paperclip invDocPaperclip : invDocPaperclips) {
            final Object attachedTo = invDocPaperclip.getAttachedTo();
            if(attachedTo instanceof Communication) {
                return true;
            }
        }
        return false;
    }

    private static String determineName(
            final Blob document,
            final String fileName) {
        String name = fileName != null ? fileName : document.getName();
        if(!name.toLowerCase().endsWith(".pdf")) {
            name = name + ".pdf";
        }
        return name;
    }

    public List<DocumentType> choices0$$() {
        return Lists.newArrayList(
                findDocumentType(Constants.DOC_TYPE_REF_SUPPLIER_RECEIPT),
                findDocumentType(Constants.DOC_TYPE_REF_TAX_RECEIPT)
                );
    }

    private DocumentType findDocumentType(final String ref) {
        return documentTypeRepository.findByReference(ref);
    }

    @Inject
    DocumentTypeRepository documentTypeRepository;

    @Inject
    PaperclipRepository paperclipRepository;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    InvoiceDocAndCommService invoiceDocAndCommService;

    @Inject
    ClockService clockService;

    @Inject
    FactoryService factoryService;

}
