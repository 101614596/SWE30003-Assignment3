class ShopApp {
    constructor() {
        this.cart = [];
        this.products = [];
        this.init();
    }

    async init() {
        await this.loadProducts();
        this.setupEventListeners();
        this.renderProducts();
    }

    async loadProducts() {
        try {
            this.products = await API.getAllProducts();
        } catch (error) {
            console.error('Failed to load products:', error);
            alert('Failed to load products. Please refresh the page.');
        }
    }

    renderProducts(productsToRender = this.products) {
        const grid = document.getElementById('product-grid');
        grid.innerHTML = '';

        productsToRender.forEach(product => {
            const card = document.createElement('div');
            card.className = 'product-card';
            card.innerHTML = `
                <h3>${product.name}</h3>
                <p class="category">${product.category}</p>
                <p>${product.description}</p>
                <p class="price">$${product.price.toFixed(2)}</p>
                <p class="stock">${product.quantity} in stock</p>
                <input type="number" min="1" max="${product.quantity}" value="1" id="qty-${product.id}">
                <button onclick="app.addToCart('${product.id}')" 
                        ${product.quantity === 0 ? 'disabled' : ''}>
                    ${product.quantity === 0 ? 'Out of Stock' : 'Add to Cart'}
                </button>
            `;
            grid.appendChild(card);
        });
    }

    async addToCart(productId) {
        const qtyInput = document.getElementById(`qty-${productId}`);
        const quantity = parseInt(qtyInput.value);

        try {
            this.cart = await API.addToCart(productId, quantity);
            this.updateCartDisplay();
            alert('Item added to cart!');
        } catch (error) {
            alert('Failed to add item to cart');
        }
    }

    updateCartDisplay() {
        const cartCount = document.getElementById('cart-count');
        const totalItems = this.cart.reduce((sum, item) => sum + item.quantity, 0);
        cartCount.textContent = totalItems;

        // Update cart section
        const cartItems = document.getElementById('cart-items');
        if (this.cart.length === 0) {
            cartItems.innerHTML = '<p>Your cart is empty</p>';
            return;
        }

        cartItems.innerHTML = '';
        let subtotal = 0;

        this.cart.forEach(item => {
            const itemTotal = item.product.price * item.quantity;
            subtotal += itemTotal;

            const cartItem = document.createElement('div');
            cartItem.className = 'cart-item';
            cartItem.innerHTML = `
                <div>
                    <h4>${item.product.name}</h4>
                    <p>$${item.product.price.toFixed(2)} x ${item.quantity}</p>
                </div>
                <div>
                    <strong>$${itemTotal.toFixed(2)}</strong>
                    <button onclick="app.removeFromCart('${item.product.id}')">Remove</button>
                </div>
            `;
            cartItems.appendChild(cartItem);
        });

        const tax = subtotal * 0.10;
        const total = subtotal + tax;

        document.getElementById('cart-subtotal').textContent = subtotal.toFixed(2);
        document.getElementById('cart-tax').textContent = tax.toFixed(2);
        document.getElementById('cart-total').textContent = total.toFixed(2);
    }

    async removeFromCart(productId) {
        try {
            this.cart = await API.removeFromCart(productId);
            this.updateCartDisplay();
        } catch (error) {
            alert('Failed to remove item');
        }
    }

    setupEventListeners() {
        // Search
        document.getElementById('search').addEventListener('input', (e) => {
            const term = e.target.value.toLowerCase();
            const filtered = this.products.filter(p =>
                p.name.toLowerCase().includes(term) ||
                p.description.toLowerCase().includes(term)
            );
            this.renderProducts(filtered);
        });

        // Category filter
        document.getElementById('category-filter').addEventListener('change', async (e) => {
            if (e.target.value === '') {
                this.renderProducts();
            } else {
                const filtered = await API.searchProducts(e.target.value);
                this.renderProducts(filtered);
            }
        });

        // Navigation
        document.getElementById('cart-link').addEventListener('click', (e) => {
            e.preventDefault();
            document.getElementById('products').classList.add('hidden');
            document.getElementById('cart').classList.remove('hidden');
        });

        // Checkout
        document.getElementById('checkout-btn').addEventListener('click', () => {
            document.getElementById('checkout-modal').classList.remove('hidden');
        });

        document.getElementById('cancel-checkout').addEventListener('click', () => {
            document.getElementById('checkout-modal').classList.add('hidden');
        });

        document.getElementById('checkout-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.processCheckout();
        });
    }

    async processCheckout() {
        const customerData = {
            name: document.getElementById('customer-name').value,
            email: document.getElementById('customer-email').value,
            phone: document.getElementById('customer-phone').value,
            address: document.getElementById('customer-address').value
        };

        const paymentMethod = document.getElementById('payment-method').value;

        try {
            const invoice = await API.checkout(customerData, paymentMethod);

            // Hide checkout modal
            document.getElementById('checkout-modal').classList.add('hidden');

            // Show invoice
            this.displayInvoice(invoice);

            // Clear cart
            this.cart = [];
            this.updateCartDisplay();

        } catch (error) {
            alert('Checkout failed: ' + error.message);
        }
    }

    displayInvoice(invoice) {
        const content = document.getElementById('invoice-content');
        content.innerHTML = `
            <h3>Order #${invoice.orderId}</h3>
            <p>Thank you for your order!</p>
            <p>Tracking: ${invoice.trackingNumber}</p>
            <p>Total: $${invoice.total.toFixed(2)}</p>
        `;
        document.getElementById('invoice-modal').classList.remove('hidden');

        document.getElementById('close-invoice').onclick = () => {
            document.getElementById('invoice-modal').classList.add('hidden');
        };
    }
}

// Initialize app
const app = new ShopApp();