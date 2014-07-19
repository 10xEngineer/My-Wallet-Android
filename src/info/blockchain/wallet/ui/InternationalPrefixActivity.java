package info.blockchain.wallet.ui;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
//import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.RadioButton;
import android.widget.Spinner;

import piuk.blockchain.android.R;

public class InternationalPrefixActivity extends Activity {
	
	private static String[] mCountries = {
		"412;af;93;null;yes;Afghanistan;;",
		"276;al;355;null;yes;Albania;;",
		"603;dz;213;null;yes;Algeria;;",
		"544;as;684;null;yes;American Samoa;",
		"213;ad;376;null;yes;Andorra;",
		"365;ai;264;null;yes;Anguilla;",
		"344;ag;268;18664402345;no;Antigua and Barbuda;",
		"722;ar;54;null;yes;Argentina;",
		"283;am;374;null;yes;Armenia;",
		"363;aw;297;null;yes;Aruba;",
		"505;au;61;1800198154;no;Australia;",
		"232;at;43;019280406;no;Austria;",
		"400;az;994;null;yes;Azerbaijan;",
		"364;bs;242;null;yes;Bahamas;",
		"426;bh;973;null;yes;Bahrain;",
		"470;bd;880;null;yes;Bangladesh;",
		"342;bb;246;null;yes;Barbados;",
		"257;by;375;null;yes;Belarus;",
		"206;be;32;024006956;no;Belgium;",
		"702;bz;501;null;yes;Belize;",
		"616;bj;229;null;yes;Benin;",
		"350;bm;441;null;yes;Bermuda;",
		"402;bt;975;null;yes;Bhutan;",
		"736;bo;591;null;yes;Bolivia;",
		"652;bw;267;null;yes;Botswana;",
		"724;br;55;null;yes;Brazil;",
		"528;bn;673;null;yes;Brunei Darussalam;",
		"284;bg;359;null;yes;Bulgaria;",
		"613;bf;226;null;yes;Burkina Faso;",
		"642;bi;257;null;yes;Burundi;",
		"456;kh;855;null;yes;Cambodia;",
		"624;cm;237;null;yes;Cameroon;",
		"302;ca;1;18664276291;no;Canada;+15148002051",
		"625;cv;238;null;yes;Cape Verde;",
		"346;ky;345;null;yes;Cayman Islands;",
		"623;cf;236;null;yes;Central African Republic;",
		"622;td;235;null;yes;Chad;",
		"730;cl;56;12300204630;no;Chile;",
		"460;cn;86;108001201911;no;China;",
		"732;co;57;018009156675;no;Colombia;",
		"654;km;269;null;yes;Comoros;",
		"629;cg;243;null;yes;Congo;",
		"548;ck;682;null;yes;Cook Islands;",
		"712;cr;506;08000121617;no;Costa Rica;",
		"612;ci;225;null;yes;Cote d'Ivoire;",
		"219;hr;385;null;yes;Croatia;",
		"368;cu;53;null;yes;Cuba;",
		"280;cy;357;null;yes;Cyprus;",
		"230;cz;420;null;yes;Czech Republic;",
		"238;dk;45;80886711;no;Denmark;+4578774102",
		"638;dj;253;null;yes;Djibouti;",
		"366;dm;767;null;yes;Dominica;",
		"370;do;809;18887518534;no;Dominican Republic;",
		"740;ec;593;null;yes;Ecuador;",
		"602;eg;20;null;yes;Egypt;",
		"706;sv;503;8006450;no;El Salvador;",
		"627;gq;240;null;yes;Equatorial Guinea;",
		"657;er;291;null;yes;Eritrea;",
		"248;ee;372;8000111017;no;Estonia;",
		"636;et;251;null;yes;Ethiopia;",
		"288;fo;298;null;yes;Faroe Islands;",
		"542;fj;679;null;yes;Fiji;",
		"244;fi;358;0800919307;no;Finland;",
//		"208;fr;33;0176743678;no;France;+33180189962",	// France toll-free 0805100081
		"208;fr;33;0176743678;no;France;0180189962",	// France toll-free 0805100081
		"742;gf;594;null;yes;French Guiana;",
		"547;pf;689;null;yes;French Polynesia;",
		"628;ga;241;null;yes;Gabon;",
		"607;gm;220;null;yes;Gambia;",
		"282;ge;995;null;yes;Georgia;",
		"262;de;49;06922221644;no;Germany;",
		"620;gh;233;null;yes;Ghana;",
		"266;gi;350;null;yes;Gibraltar;",
		"202;gr;30;00800127593;no;Greece;",
		"290;gl;299;null;yes;Greenland;",
		"352;gd;473;null;yes;Grenada;",
		"535;gu;671;null;yes;Guam;",
		"704;gt;502;18006240075;no;Guatemala;",
		"632;gw;245;null;yes;Guinea-Bissau;",
		"611;gn;224;null;yes;Guinea;",
		"738;gy;592;18665049143;no;Guyana;",
		"372;ht;509;null;yes;Haiti;",
//		"225;see;379;null;yes;",			// ??? 'see' should be 'va'
		"225;va;379;null;yes;Holy See (Vatican City State);",
		"708;hn;504;null;yes;Honduras;",
		"454;hk;852;800901972;no;Hong Kong;",
		"216;hu;36;0680016353;no;Hungary;",
		"274;is;354;8008465;no;Iceland;",
		"405;in;91;null;yes;India;",
		"510;id;62;0018030114138;no;Indonesia;",
		"432;ir;98;null;yes;Iran, Islamic Republic of;",
		"418;iq;964;null;yes;Iraq;",
		"272;ie;353;012460303;no;Ireland;",
		"425;il;972;1809216305;no;Israel;",
		"222;it;39;0236009037;no;Italy;",
		"338;jm;809;null;yes;Jamaica;",
		"441;jp;81;004422132772;no;Japan;",
		"416;jo;962;null;yes;Jordan;",
		"639;ke;254;null;yes;Kenya;",
		"545;ki;686;null;yes;Kiribati;",
		"419;kw;965;null;yes;Kuwait;",
		"437;kg;996;null;yes;Kyrgyzstan;",
		"457;la;856;null;yes;Laos;",
		"247;lv;371;8002761;no;Latvia;",
		"415;lb;961;null;yes;Lebanon;",
		"651;ls;266;null;yes;Lesotho;",
		"618;lr;231;null;yes;Liberia;",
		"606;ly;218;null;yes;Libyan Arab Jamahiriya;",
		"295;li;423;null;yes;Liechtenstein;",
		"246;lt;370;null;yes;Lithuania;",
		"270;lu;352;80026572;no;Luxembourg;",
		"455;mo;853;null;yes;Macao;",
		"294;mk;389;null;yes;Macedonia, the former Yugoslav Republic of;",
		"646;mg;261;null;yes;Madagascar;",
		"650;mw;265;null;yes;Malawi;",
		"502;my;60;1800813791;no;Malaysia;",
		"472;mv;960;null;yes;Maldives;",
		"610;ml;223;null;yes;Mali;",
		"278;mt;356;null;yes;Malta;",
		"551;mh;692;null;yes;Marshall Islands;",
		"340;mq;596;null;yes;Martinique;",
		"609;mr;222;null;yes;Mauritania;",
		"617;mu;230;null;yes;Mauritius;",
		"334;mx;52;0018665580254;no;Mexico;",
		"259;md;373;null;yes;Moldova, Republic of;",
		"212;mc;377;null;yes;Monaco;",
		"428;mn;976;null;yes;Mongolia;",
		"297;me;382;null;yes;Montenegro;",
		"354;ms;664;null;yes;Montserrat;",
		"604;ma;212;null;yes;Morocco;",
		"643;mz;258;null;yes;Mozambique;",
		"414;mm;95;null;yes;Myanmar;",
		"649;na;264;null;yes;Namibia;",
		"536;nr;674;null;yes;Nauru;",
		"429;np;977;null;yes;Nepal;",
		"362;an;599;0018665580256;no;Netherlands Antilles;",
		"204;nl;31;08000201485;no;Netherlands;",
		"546;nc;687;null;yes;New Caledonia;",
		"530;nz;64;0800444312;no;New Zealand;",
		"710;ni;505;null;yes;Nicaragua;",
		"614;ne;227;null;yes;Niger;",
		"621;ng;234;null;yes;Nigeria;",
		"534;mp;670;null;yes;Northern Mariana Islands;",
		"467;kp;850;null;yes;North Korea;",
		"242;no;47;80019717;no;Norway;",
		"422;om;968;null;yes;Oman;",
		"410;pk;92;null;yes;Pakistan;",
		"552;pw;680;null;yes;Palau;",
		"423;ps;970;null;yes;Palestinian Territory;",
		"714;pa;507;null;yes;Panama;",
		"537;pg;675;null;yes;Papua New Guinea;",
		"744;py;595;null;yes;Paraguay;",
		"716;pe;51;null;yes;Peru;",
		"515;ph;63;180011141916;no;Philippines;",
		"260;pl;48;008001212930;no;Poland;",
		"268;pt;351;800814366;no;Portugal;",
		"330;pr;787;null;yes;Puerto Rico;",
		"427;qa;974;null;yes;Qatar;",
		"647;re;262;null;yes;Reunion;",
		"226;ro;40;null;yes;Romania;",
		"250;ru;7;null;yes;Russian Federation;",
		"635;rw;250;null;yes;Rwanda;",
		"356;kn;869;null;yes;Saint Kitts and Nevis;",
		"358;lc;758;null;yes;Saint Lucia;",
		"308;pm;508;null;yes;Saint Pierre and Miquelon;",
		"360;vc;784;null;yes;Saint Vincent and the Grenadines;",
		"549;ws;685;null;yes;Samoa;",
		"292;sm;378;null;yes;San Marino;",
		"626;st;239;null;yes;Sao Tome and Principe;",
		"420;sa;966;null;yes;Saudi Arabia;",
		"608;sn;221;null;yes;Senegal;",
		"220;rs;381;null;yes;Serbia;",
		"633;sc;248;null;yes;Seychelles;",
		"619;sl;232;null;yes;Sierra Leone;",
		"525;sg;65;8001204931;no;Singapore;",
		"231;sk;421;0800001319;no;Slovakia;",
		"293;si;386;null;yes;Slovenia;",
		"540;sb;677;null;yes;Solomon Islands;",
		"637;so;252;null;yes;Somalia;",
		"655;za;27;0800981086;no;South Africa;",
		"450;kr;52;00798148007425;no;South Korea;",
		"214;es;34;914148280;no;Spain;",
		"413;lk;94;null;yes;Sri Lanka;",
		"634;sd;249;null;yes;Sudan;",
		"746;sr;597;null;yes;Suriname;",
		"653;sz;268;null;yes;Swaziland;",
//		"240;se;46;0842040100;no;Sweden;+46842040099",		// SE2: 0850564767
		"240;se;46;0842040100;no;Sweden;+46842040098",
		"228;ch;41;0445801006;no;Switzerland;",
		"417;sy;963;null;yes;Syrian Arab Republic;",
		"466;tw;886;null;yes;Taiwan, Province of China;",
		"436;tj;992;null;yes;Tajikistan;",
		"640;tz;255;null;yes;Tanzania, United Republic of;",
		"520;th;66;001800120666299;no;Thailand;",
		"615;tg;228;null;yes;Togo;",
		"539;to;676;null;yes;Tonga;",
		"374;tt;868;18882515168;no;Trinidad and Tobago;",
		"605;tn;216;null;yes;Tunisia;",
		"286;tr;90;null;yes;Turkey;",
		"438;tm;993;null;yes;Turkmenistan;",
		"376;tc;649;null;yes;Turks and Caicos Islands;",
		"641;ug;256;null;yes;Uganda;",
		"255;ua;380;null;yes;Ukraine;",
		"424;ae;971;null;yes;United Arab Emirates;",
		"430;ae;971;null;yes;United Arab Emirates;",
		"431;ae;971;null;yes;United Arab Emirates;",
//		"234;gb;44;08082381789;no;United Kingdom;",
		"234;gb;44;02031070268;no;United Kingdom;",	// changed 28/07/2010
		"310;us;1;18665284464;no;United States;+17187057197",
		"748;uy;598;0004135983607;no;Uruguay;",
		"434;uz;998;null;yes;Uzbekistan;",
		"541;vu;678;null;yes;Vanuatu;",
		"734;ve;58;null;yes;Venezuela;",
		"452;vn;84;null;yes;Viet Nam;",
		"348;vg;284;null;yes;Virgin Islands, British;",
		"332;vi;340;null;yes;Virgin Islands, U.S.;",
		"543;wf;681;null;yes;Wallis and Futuna;",
		"421;ye;967;null;yes;Yemen;",
		"645;zm;260;null;yes;Zambia;",
		"648;zw;263;null;yes;Zimbabwe;",
	};

    private TextView tvPrompt = null;

    private Spinner spnHomeSIM;
    private int spnSelection;
    private List<String> allcountries;
    private ArrayAdapter<String> aspnCountries;
//    private ISO2PlainText i2p;

    private String strSIMCountry = null;
    
    private String[] s1 = null;
    private String[] s2 = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_send_country);
        setTheme(android.R.style.Theme_Dialog);
        
        setTitle("Choose international dial prefix");
        
//        i2p = new ISO2PlainText();
//        tvPrompt = (TextView)findViewById(R.id.prompt);

        /*
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
            setTitle(extras.getString("prompt"));
        	tvPrompt.setText(extras.getString("prompt") + ": ");
        	strSIMCountry = extras.getString("value");
        }
        */

        spnHomeSIM = (Spinner) findViewById(R.id.homeSIM);
        spnSelection = 0;
        allcountries = new ArrayList<String>();
        String strPlain =  null;
        for(int i = 0; i < mCountries.length; i++) {
       		s1 = mCountries[i].split(";");
        	if(strSIMCountry != null && s1[0].compareTo(strSIMCountry) == 0)	{
        		spnSelection = i;
        	}
            allcountries.add(s1[5] + " (" + s1[1] + " +" + s1[2] + ")" + " [" + s1[0] + "]");
        }
        aspnCountries = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, allcountries);
        aspnCountries.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnHomeSIM.setAdapter(aspnCountries); 
        spnHomeSIM.setSelection(spnSelection, true);

        Button button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	String res = mCountries[(int)spnHomeSIM.getSelectedItemId()];
           		s2 = res.split(";");
                setResult(RESULT_OK, (new Intent()).setAction(s2[2]));
                finish();
            }
        });

        Button button2 = (Button)findViewById(R.id.cancel);
        button2.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View v) {
        		setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    public void onCheckedChanged(RadioGroup group, int checkedId) { ; }
    public void onClick(View v) { ; }
}
