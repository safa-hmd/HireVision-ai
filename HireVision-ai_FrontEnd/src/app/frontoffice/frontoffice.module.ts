import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { FrontofficeRoutingModule } from './frontoffice-routing.module';
import { FrontofficeComponent } from './frontoffice.component';
import { HomeComponent } from './home/home.component';
import { CvAnalyseComponent } from './cv-analyse/cv-analyse.component';
import { MenuComponent } from './menu/menu.component';
import { FooterComponent } from './footer/footer.component';
import { HeaderComponent } from './header/header.component';
import { JobMatchComponent } from './job-match/job-match.component';
import { ProfilComponent } from './profil/profil.component';
import { PlanCarriereComponent } from './plan-carriere/plan-carriere.component';
import { InterviewPreparationComponent } from './interview-preparation/interview-preparation.component';
import { InterviewSessionComponent } from './interview-session/interview-session.component';
import { InterviewFeedbackComponent } from './interview-feedback/interview-feedback.component';
import { SubscriptionComponent } from './subscription/subscription.component';

@NgModule({
  declarations: [
    FrontofficeComponent,
    HomeComponent,
    CvAnalyseComponent,
    MenuComponent,
    FooterComponent,
    HeaderComponent,
    JobMatchComponent,
    ProfilComponent,
    PlanCarriereComponent,
    InterviewPreparationComponent,
    InterviewSessionComponent,
    InterviewFeedbackComponent,
    SubscriptionComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,        // ← nécessaire pour routerLink dans menu
    FrontofficeRoutingModule
  ]
})
export class FrontofficeModule { }
