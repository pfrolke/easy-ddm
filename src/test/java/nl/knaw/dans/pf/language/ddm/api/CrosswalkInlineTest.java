package nl.knaw.dans.pf.language.ddm.api;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import nl.knaw.dans.common.lang.util.StreamUtil;
import nl.knaw.dans.pf.language.ddm.datehandlers.DcDateHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasAvailableHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasDateAccepteddHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasDateCopyrightedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasDateHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasDateSubmittedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasIssuedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasModiefiedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.EasValidHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsAvailableHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsCreatedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsDateAccepteddHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsDateCopyrightedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsDateSubmittedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsIssuedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsModiefiedHandler;
import nl.knaw.dans.pf.language.ddm.datehandlers.TermsValidHandler;
import nl.knaw.dans.pf.language.ddm.handlermaps.NameSpace;
import nl.knaw.dans.pf.language.ddm.handlertypes.BasicDateHandler;
import nl.knaw.dans.pf.language.ddm.handlertypes.IsoDateHandler;
import nl.knaw.dans.pf.language.emd.EasyMetadata;
import nl.knaw.dans.pf.language.xml.binding.Encoding;
import nl.knaw.dans.pf.language.xml.crosswalk.CrosswalkException;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrosswalkInlineTest
{
    private static final String NARCIS_TYPE = " xsi:type='narcis:DisciplineType'";
    private static final String W3CDTF_TYPE = " xsi:type='dcterms:W3CDTF'";

    private static final String FREE_CONTENT = "<xhtml:body><p>Hello</p></xhtml:body>";

    private static final String MINI_AUDIENCE = "D41500";
    private static final String MINI_DISCIPLINE = "easy-discipline:34";

    private static final Logger logger = LoggerFactory.getLogger(CrosswalkInlineTest.class);

    @BeforeClass
    public static void checkWebAccess() throws MalformedURLException
    {
        try
        {
            new URL(DDMValidator.SCHEMA_LOCATION).openStream();
        }
        catch (final IOException e)
        {
            // TODO slashes worden gesloopt door new URL()
            DDMValidator.instance().setSchemaLocation("file://" + new File("src/test/resources/offlineFallBackDDM.xsd").getAbsolutePath());
            throw new IllegalStateException("tests require web access for XSD");
        }
    }

    @Test
    public void invalidAccesRights() throws Exception
    {
        final String value = "STUPID";
        final String content = newEl("dc:title", "", "t") + newEl("ddm:accessRights", "", value);
        final String xml = newRoot(newProfile(content));
        final EasyMetadata emd = runTest(new Exception(), xml, 3, value);
        assertThat(emd, nullValue());
    }

    @Test
    public void invalidSyntax() throws Exception
    {
        final String notTerminated = "<ddm:accessRights>";
        final String notTerminatedToo = "<this mistake='is not reported'>";
        final String content = newProfile(notTerminated) + notTerminatedToo;
        runTest(new Exception(), newRoot(content), 2, "must be terminated", "title}' is expected");
    }

    @Test
    public void dateAvailable() throws Exception
    {
        final String content = newEl("ddm:accessRights", "", "OPEN_ACCESS") + newEl("ddm:available", "", "2014-03");
        final String profile = newProfile(content);
        runTest(new Exception(), newRoot(profile), 1, "Invalid content", "accessRights");
        // TODO assert
    }

    @Test
    public void minimalInput() throws Exception
    {
        final String profile = newMiniProfile("");
        final EasyMetadata emd = runTest(new Exception(), newRoot(profile), 0);
        checkMiniProfile(emd);
    }

    @Test
    public void cmdiCheckBoxForLanguageLiterature() throws Exception
    {
        // see also CMDIFormatChoiceWrapper.java
        final String cmdiMime = "application/x-cmdi+xml";
        final String fieldUnderTest = newEl("dc:format", "", cmdiMime);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newDcmi(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdFormat().getDcFormat().get(0).getValue(), is(cmdiMime));
    }

    @Test
    public void typedLanguage() throws Exception
    {
        final String value = "XXXXXX";
        final String fieldUnderTest = newEl("dcterms:language", " xsi:type='dcterms:ISO639-3'", value);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newDcmi(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdLanguage().getDcLanguage().get(0).getValue(), is(value));
    }

    @Test
    public void narcisTypeProperty() throws Exception
    {
        final String fieldUnderTest = newEl("dcterms:audience", NARCIS_TYPE, "D34500");
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newDcmi(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdAudience().getDisciplines().get(1).getValue(), is("easy-discipline:13"));
    }

    @Test
    public void narcisTypePropertyWithAlternativeNameSpace() throws Exception
    {
        final String audience = newEl("dcterms:audience", " xsi:type='xnarcis:DisciplineType'", "D34500");
        final String dcmi = newEl("ddm:dcmiMetadata", "", audience);
        final String xml = newRoot(newMiniProfile("") + dcmi).replace(" xmlns:narcis=", " xmlns:xnarcis=");
        final EasyMetadata emd = runTest(new Exception(), xml, 0);
        assertThat(emd.getEmdAudience().getDisciplines().get(1).getValue(), is("easy-discipline:13"));
    }

    @Test
    public void alternativeNameSpaces() throws Exception
    {
        final EasyMetadata emd = runTest(new Exception(), readFile("NameSpacePrefixVariants.xml"), 0);
        checkMiniProfile(emd);
    }

    @Test
    public void freeContentWithSchema() throws Exception
    {
        final String xml = newRootWithXhtml(newMiniProfile("") + newAdditional(FREE_CONTENT));
        final String xsd = "http://www.w3.org/2002/08/xhtml/xhtml1-strict.xsd";
        final String withXhtmlSchema = xml.replace("schemaLocation='", "schemaLocation='" + "http://www.w3.org/1999/xhtml" + " " + xsd + " ");
        // warnings about skipped elements
        final EasyMetadata emd = runTest(new Exception(), withXhtmlSchema, 2, " xhtml:body at level:3", " p at level:4");
        checkMiniProfile(emd);
    }

    @Test
    public void freeContentWithoutSchema() throws Exception
    {
        final String xml = newRootWithXhtml(newMiniProfile("") + newAdditional(FREE_CONTENT));
        // warnings about skipped elements
        final EasyMetadata emd = runTest(new Exception(), xml, 2, " xhtml:body at level:3", " p at level:4");
        checkMiniProfile(emd);
    }

    @Test
    public void freeContentWithoutNS() throws Exception
    {
        final String xml = newRoot(newMiniProfile("") + newAdditional("<body><p>Hello</p></body>"));
        // warnings about skipped elements
        final EasyMetadata emd = runTest(new Exception(), xml, 3, "Invalid content", "schemas");
        assertThat(emd, nullValue());
    }

    @Test
    public void freeContentInProfile() throws Exception
    {
        final String xml = newRootWithXhtml(newMiniProfile(FREE_CONTENT));
        final EasyMetadata emd = runTest(new Exception(), xml, 3, " xhtml:body at level:3", " p at level:4", "No child element is expected");
        assertThat(emd, nullValue());
    }

    @Test
    public void typedDcDate() throws Exception
    {
        final String value = "1919";
        final String fieldUnderTest = newEl("dc:date", W3CDTF_TYPE, value);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdDate().getEasDate().get(0).getValue(), is(new DateTime(value)));
    }

    @Test
    public void dcDate() throws Exception
    {
        final String value = "bronze age";
        final String fieldUnderTest = newEl("dc:date", "", value);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdDate().getDcDate().get(0).getValue(), is(value));
    }

    @Test
    public void typedDcTermsDate() throws Exception
    {
        final String value = "1919-01";
        final String fieldUnderTest = newEl("dcterms:date", W3CDTF_TYPE, value);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdDate().getEasDate().get(0).getValue(), is(new DateTime(value)));
    }

    @Test
    public void subTypedDcTermsDate() throws Exception
    {
        final String value = "1919";
        final String fieldUnderTest = newEl("dcterms:date", " xsi:type='xs:gYear'", value);
        // TODO Would it work with more name space declarations in the root element?
        runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(fieldUnderTest)), 5, "prefix 'xs'");
    }

    @Test
    public void daiCreator() throws Exception
    {
        final String name = "pipo de clown";
        final String id = "989691736x";
        final String sys = "info:eu-repo/dai/nl/";
        final String attribute = " DAI='" + sys + id + "'";
        final String fieldUnderTest = newEl("dcx-dai:creator", attribute, name);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdCreator().getEasCreator().get(0).getSurname(), is(name));
        assertThat(emd.getEmdCreator().getEasCreator().get(0).getEntityId(), is(id));
        assertThat(emd.getEmdCreator().getEasCreator().get(0).getIdentificationSystem().toString(), is(sys));
    }

    @Test
    public void dcTermsDate() throws Exception
    {
        final String value = "bronze age";
        final String fieldUnderTest = newEl("dcterms:date", "", value);
        final EasyMetadata emd = runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(fieldUnderTest)), 0);
        checkMiniProfile(emd);
        assertThat(emd.getEmdDate().getDcDate().get(0).getValue(), is(value));
    }

    @Test
    public void spatialPoint() throws Exception
    {
        // TODO XSD validator does not load auxiliary schema's
        // final EasyMetadata emd =
        runTest(new Exception(), readFile("spatial.xml"), 2, "Cannot resolve", "Point");
        // TODO fix multiple XSD's
        // checkMiniProfile(emd);
        // assertThat(emd.getEmdCoverage().getEasSpatial().get(0).getPoint().getX(), is("2.0"));
        // assertThat(emd.getEmdCoverage().getEasSpatial().get(0).getPoint().getY(), is("1.0"));
        // assertThat(emd.getEmdCoverage().getEasSpatial().get(0).getPoint().getScheme(),is(SpatialPointHandler.WGS84_4326));
    }

    @Test
    public void abr() throws Exception
    {
        // TODO XSD validator does not load auxiliary schema's
        // final EasyMetadata emd =
        runTest(new Exception(), readFile("abr.xml"), 4, "Cannot resolve", "dc:subject");
        // TODO fix multiple XSD's
        // checkMiniProfile(emd);
    }
    private static final String fields[] = {"dcterms:created","dcterms:available","dcterms:dateAccepted","dcterms:valid","dcterms:issued","dcterms:modified","dcterms:dateCopyrighted","dcterms:dateSubmitted","dcterms:date","dc:date"};

    @Test
    public void emptyDates() throws Exception
    {
        StringBuffer sb = new StringBuffer();
        for (String field : fields){
            sb.append(newEl(field,  "",""));
        }
        runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(sb.toString())), 0);
    }

    @Test
    public void emptyW3cDates() throws Exception
    {
        StringBuffer sb = new StringBuffer();
     // TODO  sb.append(newEl("ddm:created", "",""));
     // TODO sb.append(newEl("ddm:available",  "",""));
        
        for (String field : fields){
            sb.append(newEl(field,  W3CDTF_TYPE,""));
        }
        runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(sb.toString())), fields.length*2,"must be valid");
    }

    @Test
    public void dates() throws Exception
    {
    	StringBuffer sb = new StringBuffer();
       // TODO sb.append(newEl("ddm:created", "","2013"));
       // TODO sb.append(newEl("ddm:available",  "","1900"));
        
        for (String field : fields){
        	sb.append(newEl(field,  W3CDTF_TYPE,"190003"));
        	sb.append(newEl(field,  "","03-2013"));
        }
        runTest(new Exception(), newRoot(newMiniProfile("") + newAdditional(sb.toString())), 0);
    }

    private String readFile(final String string) throws Exception
    {
        final byte[] xml = StreamUtil.getBytes(new FileInputStream("src/test/resources/input/" + string));
        final String string2 = new String(xml, Encoding.UTF8);
        return string2;
    }

    private static String newEl(final String element, final String attributes, final String content)
    {
        return "<" + element + attributes + ">" + content + "</" + element + ">";
    }

    private static String newProfile(final String content)
    {
        return newEl("ddm:profile", "", content);
    }

    private static String newMiniProfile(final String additionalContent)
    {
        final String defaultContent = ""//
                + newEl("dc:title", "", "fabeltjeskrant") //
                + newEl("dcterms:description", "", "tv serie") //
                + newEl("dc:creator", "", "meneer de uil") //
                + newEl("ddm:created", "", "2013") //
                + newEl("ddm:audience", "", MINI_AUDIENCE) //
                + newEl("ddm:accessRights", "", "OPEN_ACCESS");

        return newEl("ddm:profile", "", defaultContent + additionalContent);
    }

    private static String newDcmi(final String content)
    {
        return newEl("ddm:dcmiMetadata", "", content);
    }

    private static String newAdditional(final String content)
    {
        return newEl("ddm:additional-xml", "", content);
    }

    private static String newRoot(final String content)
    {
        return "<?xml version='1.0' encoding='UTF-8'?>\n" + newEl("ddm:DDM", newRootAttrs(), content);
    }

    private static String newRootWithXhtml(final String content)
    {
        final String attrs = newRootAttrs() + " " + "xmlns:xhtml='" + "http://www.w3.org/1999/xhtml" + "'";
        return "<?xml version='1.0' encoding='UTF-8'?>\n" + newEl("ddm:DDM", attrs, content);
    }

    private static String newRootAttrs()
    {
        final StringBuffer attrs = new StringBuffer();
        for (final NameSpace ns : NameSpace.values())
            attrs.append(" xmlns:" + ns.prefix + "='" + ns.uri + "'");
        attrs.append(" xsi:schemaLocation='" + NameSpace.DDM.uri + " " + NameSpace.DDM.xsd + "'");
        return attrs.toString();
    }

    private static void checkMiniProfile(final EasyMetadata emd)
    {
        assertThat(emd.getEmdAudience().getDisciplines().get(0).getValue(), is(MINI_DISCIPLINE));
        assertThat(emd.getEmdTitle().getDcTitle().get(0).getValue(), is("fabeltjeskrant"));
        assertThat(emd.getEmdDescription().getDcDescription().get(0).getValue(), is("tv serie"));
        assertThat(emd.getEmdCreator().getDcCreator().get(0).getValue(), is("meneer de uil"));
        assertThat(emd.getEmdDate().getEasCreated().get(0).getValue(), is(new DateTime("2013")));
        assertThat(emd.getEmdRights().getTermsAccessRights().get(0).getValue(), is("OPEN_ACCESS"));
    }

    private static EasyMetadata runTest(final Exception dummyException, final String xml, final int i, final String... messageContents)
    {
        final String methodName = dummyException.getStackTrace()[0].getMethodName();
        final Ddm2EmdCrosswalk crosswalk = new Ddm2EmdCrosswalk();
        EasyMetadata emd = null;
        logger.debug(methodName + "\n" + xml);
        try
        {
            emd = crosswalk.createFrom(xml);
        }
        catch (final CrosswalkException e)
        {
            logger.info(methodName + " " + e.getMessage());
            assertThat(e.getMessage(), containsString(messageContents[0]));
        }
        logger.debug(crosswalk.getXmlErrorHandler().getMessages());
        for (final String m : messageContents)
            assertThat(crosswalk.getXmlErrorHandler().getMessages(), containsString(m));
        assertThat(crosswalk.getXmlErrorHandler().getNotificationCount(), is(i));
        return emd;
    }
}
