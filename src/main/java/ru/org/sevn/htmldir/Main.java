/*
 * Copyright 2016 Veronica Anokhina.
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
package ru.org.sevn.htmldir;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
    
    private static OutFiles of = new OutFiles();
    static class OutFiles {
        boolean epub = !true;
        boolean A4inchLandscape = true;
        boolean A4inchPortrait = true;
        boolean A4inchLandscape_ = true;
        boolean A4inchPortrait_ = !true;
        boolean dat = !true;
    }
    
    //"C:/Portable/progs/Calibre Portable/Calibre/ebook-convert.exe" 
    public static final String CALIBRE_CONVERTER = "C:/Portable/progs/Calibre Portable/Calibre/ebook-convert.exe";
    public static String A4inchLandscape = "11.692x8.267";
    public static String A4inchPortrait = "8.267x11.692";
    
    public static final String ENC = "UTF-8";
    public static final String OUTFILE_POSTFIX = "";
    public static final String INDEX_FILE_NAME = "index_sevn_HD.html";
    
    public static void main(String[] args) throws IOException {
        File dirr = new File("C:/pub/scrap"); 
        processDir(false, "", "/", dirr);
    }
    
    public static boolean isIndexHtml(final String str) { 
        return str.equals("index.html") || str.equals("index.htm");
    }
    
    public static void setOutputHtmlSettings(Document.OutputSettings os) {
        os.charset(ENC);
        os.prettyPrint(false);
    }
    
    public static String processDir(boolean makeIndexHtml, String prefix, String delim, File dirr) throws UnsupportedEncodingException, IOException {
        if (dirr.exists() && dirr.isDirectory() && dirr.canRead() && dirr.canWrite()) {
            boolean hasIndex = false;
            String ret = INDEX_FILE_NAME;
            Document document = null;
            
            if (makeIndexHtml) {
                document = Jsoup.parse("<html><head><meta charset=\""+ENC+"\" /></head></html>");
                setOutputHtmlSettings(document.outputSettings());
                document.title(prefix + delim + dirr.getName());
                Element h1 = document.body().appendElement("h1");
                h1.append(prefix+delim);
                Element h1a = h1.appendElement("a");
                h1a.attr("href", "..");
                h1a.html(dirr.getName());
                //document.body().appendElement("h1").html(prefix + delim + dirr.getName());
            }
            
            File[] dirList = dirr.listFiles();
            ArrayList<File> dirListFiles = new ArrayList();
            ArrayList<File> dirListDirs = new ArrayList();
            ArrayList<File> dirListIdx = new ArrayList();
            for (File fl : dirList) {
                if (fl.isDirectory()) {
                    dirListDirs.add(fl);
                } else {
                    if (isIndexHtml(fl.getName().toLowerCase())) {
                        dirListIdx.add(fl);
                    } else {
                        dirListFiles.add(fl);
                    }
                }
            }
            dirListFiles.addAll(dirListDirs);
            dirListFiles.addAll(dirListIdx);
            
            for (File fl : dirListFiles) {
                if (!fl.isHidden()) {
                    String linkTitle = fl.getName();
                    String linkRef = fl.getName();

                    if (fl.isDirectory()) {
                        linkRef += (delim + processDir(makeIndexHtml, prefix+delim+dirr.getName(), delim, fl));
                    } else {
                        if (isHtml(fl.getName().toLowerCase())) {
                            //Jsoup.clean(html, Whitelist.relaxed());
                            Document documentHtml = Jsoup.parse(fl, ENC);
                            setOutputHtmlSettings(documentHtml.outputSettings());
                            outFile(new File(fl.getParentFile(), fl.getName()+OUTFILE_POSTFIX), compressHtml(documentHtml.outerHtml()), ENC);
                        }
                    }
                    
                    if (isIndexHtml(fl.getName().toLowerCase())) {
                        hasIndex = true;
                        document = null;
                        ret = fl.getName();
                        // make epub
                        try {
                            File parent = fl.getParentFile();
                            File pdfFile = new File(parent.getParentFile(), parent.getName()+"-land.pdf");
                            File pdfFilePort = new File(parent.getParentFile(), parent.getName()+"-port.pdf");
                            File pdfFile_ = new File(parent.getParentFile(), parent.getName()+"-land-.pdf");
                            File pdfFilePort_ = new File(parent.getParentFile(), parent.getName()+"-port-.pdf");
                            File datFile = new File(parent.getParentFile(), parent.getName()+".dat.txt");
                            File datIndexFile = new File(parent, "index.dat");
                            
                            if (of.epub) {
                                Document documentHtml = Jsoup.parse(fl, ENC);
                                File epubFile = new File(parent.getParentFile(), parent.getName()+".epub");
                                writeEpub(
                                        html2epub(makeEpubBook(documentHtml), dirr, "index.html", documentHtml, "", "/")
                                        , epubFile);

                                if (of.A4inchLandscape) epub2pdf(epubFile, pdfFile, A4inchLandscape);
                                if (of.A4inchPortrait) epub2pdf(epubFile, pdfFilePort, A4inchPortrait);
                            }
                            if (of.A4inchLandscape_) epub2pdf(fl, pdfFile_, A4inchLandscape);
                            if (of.A4inchPortrait_) epub2pdf(fl, pdfFilePort_, A4inchPortrait);
                            if (of.dat) Files.copy(datIndexFile.toPath(), datFile.toPath());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return ret;
                    }
                    
                    if (document != null) {
                        Element link = document.body().appendElement("a");
                        link.attr("href", linkRef);
                        link.append(linkTitle);
                    }
                }
            }
            if (makeIndexHtml && !hasIndex && document != null) {
                outFile(new File(dirr, INDEX_FILE_NAME), document.html(), ENC);
            }
            return ret;
        }
        return "";
    }
    
    public static void outFile(File fl, String s, String encoding) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        BufferedWriter bwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fl), encoding));
        try {
            bwriter.write(s);
        } finally {
            bwriter.close();
        }
    }

    private static boolean isHtml(final String toLowerCase) {
        return toLowerCase.endsWith(".html") || toLowerCase.endsWith(".htm");
    }
    
    private static String compressHtml(String s) {
        HtmlCompressor compr = new HtmlCompressor();
        compr.setCompressCss(true);
        compr.setCompressJavaScript(true);
        return compr.compress(s);
    }
    
    private static Book makeEpubBook(Document doc) throws IOException {
        //http://www.siegmann.nl/epublib/example-programs/epub-sample-simple1
        //http://www.siegmann.nl/epublib
        Book book = new Book();
        if (doc.title() != null) {
            book.getMetadata().addTitle(doc.title());
        }
        Elements authMeta = doc.select("meta[name=author]");
        if (authMeta != null) {
            for(Element el : authMeta) {
                String auth = el.attr("content");
                if (auth != null) {
                    book.getMetadata().addAuthor(new Author(auth));
                }
            }
        }
        // Set cover image book.getMetadata().setCoverImage
        return book;
    }
    
    private static Book html2epub(Book book, File docDir, String htmlFileName, Document doc, String prefix, String pathdelim) throws IOException {
        
        for (File fl : docDir.listFiles()) {
            if (fl.isHidden()) {
                
            } else 
            if (fl.isDirectory()) {
                html2epub(book, fl, htmlFileName, null, prefix + pathdelim + fl.getName(), pathdelim);
            } else {
                if (prefix.length() == 0 && isIndexHtml(fl.getName().toLowerCase())) {
                    TOCReference mainPart = book.addSection(doc.title(), new Resource(doc.outerHtml().getBytes(ENC), htmlFileName));
                } else {
                    book.getResources().add(new Resource(new FileInputStream(new File(docDir, fl.getName())), prefix + fl.getName()));
                }
            }
        }
        
        /*
        Elements imports = doc.select("link[href]");
        for (Element el : imports) {
            String fileName = el.attr("href");
            try {
                book.getResources().add(new Resource(new FileInputStream(new File(docDir, fileName)), fileName));
            } catch (Exception e) {}
        }
        //link.attr("abs:href")
        
        Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
        for (Element el : images) {
            String fileName = el.attr("src");
            try {
                book.getResources().add(new Resource(new FileInputStream(new File(docDir, fileName)), fileName));
            } catch (Exception e) {}
        } 
        */

        //TOCReference mainPart = book.addSection(doc.title(), new Resource(doc.outerHtml().getBytes(ENC), htmlFileName));
        
        /*
        Elements atag = doc.select("a[href]");
        for (Element el : atag) {
            String fileName = el.attr("href");
            try {
                html2epub(book, docDir, fileName, Jsoup.parse(fileName));
            } catch (Exception e) {}
        } 
                */
        return book;
    }
    private static void writeEpub(Book book, File fileEPub) throws IOException {
        EpubWriter epubWriter = new EpubWriter();
        epubWriter.write(book, new FileOutputStream(fileEPub));
    }

    //https://manual.calibre-ebook.com/generated/en/ebook-convert.html#pdf-output-options
    private static void epub2pdf(File inFile, File pdfFile, String customSize) throws IOException {
        ProcessBuilder pbuilder = new ProcessBuilder(CALIBRE_CONVERTER, inFile.getAbsolutePath(), pdfFile.getAbsolutePath(), "--custom-size", customSize);
        pbuilder.inheritIO();
        pbuilder.start();
    }
}
