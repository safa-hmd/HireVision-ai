import { AfterViewInit, Component, OnInit } from '@angular/core';
import { SubscriptionService, SubscriptionInfo, PaymentTransaction } from '../../services/subscription.service';
import { AuthService } from '../../services/auth.service';

declare const lucide: any;
declare function showToast(message: string, type?: string): void;
declare function confirmAction(message: string, onConfirm: () => void): void;

interface Plan {
  key: 'PRO' | 'PREMIUM';
  name: string;
  price: number;
  tagline: string;
  features: string[];
  highlighted?: boolean;
}

@Component({
  selector: 'app-subscription',
  templateUrl: './subscription.component.html',
  styleUrls: ['./subscription.component.css']
})
export class SubscriptionComponent implements OnInit, AfterViewInit {

  plans: Plan[] = [
    {
      key: 'PRO',
      name: 'Pro',
      price: 29,
      tagline: 'Pour un candidat qui postule activement',
      features: [
        'Analyses de CV illimitées',
        'Matching avec les offres d\'emploi',
        'Simulations d\'entretien illimitées',
        'Plan de carrière personnalisé'
      ]
    },
    {
      key: 'PREMIUM',
      name: 'Premium',
      price: 59,
      tagline: 'Pour maximiser vos chances',
      highlighted: true,
      features: [
        'Tout le plan Pro',
        'Analyse vocale et comportementale avancée',
        'Feedback IA détaillé après chaque entretien',
        'Support prioritaire'
      ]
    }
  ];

  currentSubscription: SubscriptionInfo | null = null;
  payments: PaymentTransaction[] = [];
  isLoading = false;

  // État du modal de paiement simulé
  isPaymentModalOpen = false;
  selectedPlan: Plan | null = null;
  isProcessing = false;

  // Champs du formulaire de carte (paiement simulé — aucune donnée n'est transmise à une vraie banque)
  cardName = '';
  cardNumber = '';
  cardExpiry = '';
  cardCvc = '';

  constructor(
    private subscriptionService: SubscriptionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadSubscriptionInfo();
  }

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  loadSubscriptionInfo(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;

    this.isLoading = true;
    this.subscriptionService.getByUserId(userId).subscribe({
      next: (subs) => {
        this.currentSubscription = subs.find(s => s.status === 'ACTIVE') || null;
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 50);
      },
      error: () => { this.isLoading = false; }
    });

    this.subscriptionService.getPaymentsByUserId(userId).subscribe({
      next: (payments) => this.payments = payments,
      error: () => this.payments = []
    });
  }

  choosePlan(plan: Plan): void {
    if (this.currentSubscription?.plan === plan.key) {
      showToast('Vous êtes déjà abonné à ce plan', 'info');
      return;
    }
    this.selectedPlan = plan;
    this.cardName = '';
    this.cardNumber = '';
    this.cardExpiry = '';
    this.cardCvc = '';
    this.isPaymentModalOpen = true;
  }

  closePaymentModal(): void {
    if (this.isProcessing) return;
    this.isPaymentModalOpen = false;
  }

  isCardFormValid(): boolean {
    return this.cardName.trim().length > 1
      && /^[0-9\s]{13,19}$/.test(this.cardNumber.trim())
      && /^\d{2}\/\d{2}$/.test(this.cardExpiry.trim())
      && /^\d{3,4}$/.test(this.cardCvc.trim());
  }

  confirmPayment(): void {
    if (!this.selectedPlan) return;
    if (!this.isCardFormValid()) {
      showToast('Merci de vérifier les informations de la carte', 'warning');
      return;
    }

    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      showToast('Utilisateur non connecté', 'danger');
      return;
    }

    this.isProcessing = true;

    // Paiement simulé : aucune donnée de carte n'est envoyée, seul l'abonnement est créé
    // côté serveur avec un paiement marqué "PAID" (voir SubscriptionServiceImpl côté backend).
    this.subscriptionService.subscribe(userId, this.selectedPlan.key).subscribe({
      next: () => {
        this.isProcessing = false;
        this.isPaymentModalOpen = false;
        showToast(`Abonnement ${this.selectedPlan!.name} activé avec succès !`, 'success');
        this.loadSubscriptionInfo();
      },
      error: () => {
        this.isProcessing = false;
        showToast('Erreur lors du paiement, veuillez réessayer', 'danger');
      }
    });
  }

  cancelSubscription(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;

    confirmAction('Annuler votre abonnement en cours ?', () => {
      this.subscriptionService.cancel(userId).subscribe({
        next: () => {
          showToast('Abonnement annulé', 'success');
          this.loadSubscriptionInfo();
        },
        error: () => showToast('Erreur lors de l\'annulation', 'danger')
      });
    });
  }
}
