```c
case 111000:
   // Account Management Service returns error that user name is already taken
   *input = SBAPICreateAccountInputEmailAddress;
   return LocalizedString(@"Remote.Error.SignUp.Username.Taken");

case 111001:
   // Account Management Service returns error
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111005:
   *input = SBAPICreateAccountInputEmailAddress;
   return LocalizedString(@"Remote.Error.SignUp.Email.TooLong");

case 111008:
   // Missing emailAddress attribute.
   *input = SBAPICreateAccountInputEmailAddress;
   return LocalizedString(@"Remote.Error.SignUp.Email.Missing");

case 111009:
   // Missing registration source attribute.
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111011:
   // Missing password attribute.
   *input = SBAPICreateAccountInputPassword;
   return LocalizedString(@"Remote.Error.SignUp.Password.Missing");

case 111012:
   // Missing market parameter is required.
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111013:
   // Missing addressLine1 attribute.
   *input = SBAPICreateAccountInputAddress1;
   return LocalizedString(@"Remote.Error.SignUp.Address.Line1.Missing");

case 111014:
   // Missing postalCode attribute.
   *input = SBAPICreateAccountInputPostalCode;
   return LocalizedString(@"Remote.Error.SignUp.PostalCode.Missing");

case 111015:
   // Missing lastName attribute.
   *input = SBAPICreateAccountInputLastName;
   return LocalizedString(@"Remote.Error.SignUp.Name.Last.Missing");

case 111016:
   // Missing firstName attribute.
   *input = SBAPICreateAccountInputFirstName;
   return LocalizedString(@"Remote.Error.SignUp.Name.First.Missing");

case 111017:
   // Missing countrySubdivision attribute.
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111018:
   // Missing country attribute.
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.SignUp.Country.Missing");

case 111019:
   // Missing city attribute.
   *input = SBAPICreateAccountInputCity;
   return LocalizedString(@"Remote.Error.SignUp.City.Missing");

case 111020:
   // Missing birthMonth attribute. Valid values are {1-12}
   *input = SBAPICreateAccountInputBirthMonth;
   return LocalizedString(@"Remote.Error.SignUp.Birday.Month.Missing");

case 111021:
   // Missing birthDay attribute. Valid values are {1-31}
   *input = SBAPICreateAccountInputBirthDay;
   return LocalizedString(@"Remote.Error.SignUp.Birday.Day.Missing");

case 111022:
   // Account Management Service returns error that password does not meet complexity requirements
   *input = SBAPICreateAccountInputPassword;
   return LocalizedString(@"Remote.Error.SignUp.Password.Invalid");

case 111023:
   // Create Account Request was malformed.
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111027:
   // Account Management Service returns error that email address is already taken
   *input = SBAPICreateAccountInputEmailAddress;
   return LocalizedString(@"Remote.Error.SignUp.Email.Taken");

case 111035:
   *input = SBAPICreateAccountInputPostalCode;
   return LocalizedString(@"Remote.Error.SignUp.PostalCode.Invalid");

case 111036:
   *input = SBAPICreateAccountInputAnyName;
   return LocalizedString(@"Remote.Error.SignUp.Name.Invalid");

case 111039:
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111041:
   *input = SBAPICreateAccountInputEmailAddress;
   return LocalizedString(@"Remote.Error.SignUp.Email.Invalid");

case 500:
   *input = SBAPICreateAccountInputUnknown;
   return LocalizedString(@"Remote.Error.Unknown");

case 111100:
   *input = SBAPICreateAccountInputGender;
   return LocalizedString(@"Validation.Gender.Invalid");

case 111101:
   *input = SBAPICreateAccountInputLanguage;
   return LocalizedString(@"Validation.Language.Invalid");

case 111102:
   *input = SBAPICreateAccountInputBirthdate;
   return LocalizedString(@"Validation.Birthday.Invalid");

case 111103:
   *input = SBAPICreateAccountInputBirthdate;
   return LocalizedString(@"Validation.Birthday.Early");

//        case 111103:
//            *input = SBAPICreateAccountInputBirthdate;
//            return LocalizedString(@"Validation.Birthday.Early");

case 111104:
   *input = SBAPICreateAccountInputStarbucksCardNumber;
   return LocalizedString(@"Validation.StarbucksCard.Invalid");

case 211000:
case 121000:
   *input = SBAPICreateAccountInputStarbucksCardNumber;
   return LocalizedString(@"Validation.StarbucksCard.Invalid");

case 111105:
   *input = SBAPICreateAccountInputSecurityQuestion;
   return LocalizedString(@"Validation.SecurityQuestion.Invalid");

case 111106:
   *input = SBAPICreateAccountInputSecurityAnswer;
   return LocalizedString(@"Validation.SecurityAnswer.Invalid");
```
